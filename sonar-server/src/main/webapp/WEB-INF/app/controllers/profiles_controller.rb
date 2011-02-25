#
# Sonar, open source software quality management tool.
# Copyright (C) 2008-2011 SonarSource
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#
class ProfilesController < ApplicationController
  SECTION=Navigation::SECTION_CONFIGURATION

  # GETs should be safe (see http://www.w3.org/2001/tag/doc/whenToUseGet.html)
  verify :method => :post, :only => ['create', 'delete', 'copy', 'set_as_default', 'restore', 'set_projects', 'rename', 'change_parent'], :redirect_to => { :action => 'index' }

  # the backup action is allow to non-admin users : see http://jira.codehaus.org/browse/SONAR-2039
  before_filter :admin_required, :except => [ 'index', 'show', 'projects', 'permalinks', 'export', 'backup', 'inheritance' ]

  #
  #
  # GET /profiles/index
  #
  #
  def index
    @profiles = Profile.find(:all, :conditions => ['enabled=?', true], :order => 'name')
  end


  #
  #
  # GET /profiles/show/<id>
  #
  #
  def show
    @profile = Profile.find(params[:id])
  end



  #
  #
  # POST /profiles/create?name=<profile name>&language=<language>
  #
  #
  def create
    profile_name=params[:name]
    language=params[:language]
    if profile_name.blank?|| language.blank?
      flash[:warning]='Please type a profile name.'
    else
      profile=Profile.find_by_name_and_language(profile_name, language)
      if profile
        flash[:error]="This profile already exists: #{profile_name}"

      else
        profile = Profile.create(:name => profile_name, :language => language, :default_profile => false, :enabled => true)
        ok=profile.errors.empty?
        if ok && params[:backup]
          params[:backup].each_pair do |importer_key, file|
            if !file.blank? && ok
              messages = java_facade.importProfile(profile_name, language, importer_key, read_file_param(file))
              flash_validation_messages(messages)
              ok &= !messages.hasErrors()
            end
          end
        end
        if ok
          flash[:notice]= "Profile '#{profile.name}' created. Set it as default or link it to a project to use it for next measures."
        else
          profile.reload
          profile.destroy
        end
      end
    end
    redirect_to :action => 'index'
  end


  #
  #
  # POST /profiles/delete/<id>
  #
  #
  def delete
    @profile = Profile.find(params[:id])
    if @profile && @profile.deletable?
      java_facade.deleteProfile(@profile.id)
      flash[:notice]="Profile '#{@profile.name}' is deleted."
    end
    redirect_to(:controller => 'profiles', :action => 'index')
  end


  #
  #
  # POST /profiles/set_as_default/<id>
  #
  #
  def set_as_default
    profile = Profile.find(params[:id])
    profile.set_as_default
    flash[:notice]="Default profile is '#{profile.name}'."
    redirect_to :action => 'index'
  end



  #
  #
  # POST /profiles/copy/<id>?name=<name of new profile>
  #
  #
  def copy
    profile = Profile.find(params[:id])
    name = params['copy_' + profile.id.to_s]

    validation_errors = profile.validate_copy(name)
    if validation_errors.empty?
      java_facade.copyProfile(profile.id, name)
      flash[:notice]= "Profile '#{name}' is created but not activated."
    else
      flash[:error] = validation_errors.full_messages.first
    end

    redirect_to :action => 'index'
  end



  #
  #
  # POST /profiles/backup/<id>
  #
  #
  def backup
    profile = Profile.find(params[:id])
    xml = java_facade.backupProfile(profile.id)
    filename=profile.name.gsub(' ', '_')
    send_data(xml, :type => 'text/xml', :disposition => "attachment; filename=#{filename}_#{profile.language}.xml")
  end



  #
  #
  # POST /profiles/restore/<id>
  #
  #
  def restore
    if params[:backup].blank?
      flash[:warning]='Please upload a backup file.'
    else
      messages=java_facade.restoreProfile(read_file_param(params[:backup]))
      flash_validation_messages(messages)
    end
    redirect_to :action => 'index'
  end



  #
  #
  # GET /profiles/export?name=<profile name>&language=<language>&format<exporter key>
  #
  #
  def export
    language = params[:language]
    if (params[:name].blank?)
      profile = Profile.find_active_profile_by_language(language)
    else
      profile = Profile.find_by_name_and_language(CGI::unescape(params[:name]), language)
    end
    exporter_key = params[:format]
    result = java_facade.exportProfile(profile.id, exporter_key)
    send_data(result, :type => java_facade.getProfileExporterMimeType(exporter_key), :disposition => 'inline')
  end

  #
  #
  # GET /profiles/inheritance?id=<profile id>
  #
  #
  def inheritance
    @profile = Profile.find(params[:id])
    
    profiles=Profile.find(:all, :conditions => ['language=? and id<>? and (parent_name is null or parent_name<>?) and enabled=?', @profile.language, @profile.id, @profile.name, true], :order => 'name')
    @select_parent = [['None', nil]] + profiles.collect{ |profile| [profile.name, profile.name] }
  end
  
  #
  #
  # GET /profiles/changelog?id=<profile id>
  #
  #
  def changelog
    @profile = Profile.find(params[:id])
    
    @changes=ActiveRuleChange.find(:all, :conditions => ['profile_id=?', @profile.id], :order => 'id desc')
    
  end


  #
  #
  # POST /profiles/change_parent?id=<profile id>&parent_name=<parent profile name>
  #
  #
  def change_parent
    id = params[:id].to_i
    parent_name = params[:parent_name]
    if parent_name.blank?
      messages = java_facade.changeParentProfile(id, nil, current_user.login)
    else
      messages = java_facade.changeParentProfile(id, parent_name, current_user.login)
    end
    flash_validation_messages(messages)
    redirect_to :action => 'inheritance', :id => id
  end


  #
  #
  # GET /profiles/permalinks?id=<profile id>
  #
  #
  def permalinks
    @profile = Profile.find(params[:id])
  end


  #
  #
  # GET /profiles/projects/<id>
  #
  #
  def projects
    @profile = Profile.find(params[:id])
    @available_projects=Project.find(:all, 
      :include => ['profile','snapshots'], 
      :conditions => ['projects.qualifier=? AND projects.scope=? AND snapshots.islast=?', Project::QUALIFIER_PROJECT, Project::SCOPE_SET, true],
      :order => 'projects.name asc')
    @available_projects-=@profile.projects
  end



  #
  #
  # POST /profiles/set_projects/<id>?projects=<project ids>
  #
  #
  def set_projects
    @profile = Profile.find(params[:id])
    @profile.projects.clear

    projects=Project.find(params[:projects] || [])
    @profile.projects=projects
    flash[:notice]="Profile '#{@profile.name}' associated to #{projects.size} projects."
    redirect_to :action => 'projects', :id => @profile.id
  end



  #
  #
  # POST /profiles/rename/<id>?name=<new name>
  #
  #
  def rename
    profile = Profile.find(params[:id])
    name = params['rename_' + profile.id.to_s]

    if name.blank?
      flash[:warning]='Profile name can not be blank.'
    else
      existing=Profile.find(:first, :conditions => {:name => name, :language => profile.language, :enabled => true})
      if existing
        flash[:warning]='This profile name already exists.'
      elsif !profile.provided?
        java_facade.renameProfile(profile.id, name)
      end
    end
    redirect_to :action => 'index'
  end


  private

  def read_file_param(configuration_file)
    # configuration file is a StringIO
    if configuration_file.respond_to?(:read)
      return configuration_file.read
    end
    # configuration file is not a readable object
    nil
  end

  def flash_validation_messages(messages)
    # only 4 messages are kept each time to avoid cookie overflow.
    if messages.hasErrors()
      flash[:error]=messages.getErrors().to_a[0...4].join('<br/>')
    end
    if messages.hasWarnings()
      flash[:warning]=messages.getWarnings().to_a[0...4].join('<br/>')
    end
    if messages.hasInfos()
      flash[:notice]=messages.getInfos().to_a[0...4].join('<br/>')
    end
  end
end
