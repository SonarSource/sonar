define [
  'backbone.marionette',
  'templates/api-documentation'
  'api-documentation/models/web-service-action',
  'api-documentation/views/api-documentation-action-view'
], (
  Marionette,
  Templates
  WebServiceAction,
  ApiDocumentationActionView
) ->

  class ApiDocumentationActionsListView extends Marionette.CompositeView
    tagName: 'div'
    className: 'api-documentation-actions'
    template: Templates['api-documentation-actions']
    itemView: ApiDocumentationActionView
    itemViewContainer: '.api-documentation-actions-list'
