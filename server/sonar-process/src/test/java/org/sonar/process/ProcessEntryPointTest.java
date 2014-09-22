/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.process;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Timeout;
import org.sonar.process.test.StandardProcess;

import java.io.File;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;

public class ProcessEntryPointTest {

  SystemExit exit = mock(SystemExit.class);

  /**
   * Safeguard
   */
  @Rule
  public Timeout timeout = new Timeout(10000);

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void load_properties_from_file() throws Exception {
    File propsFile = temp.newFile();
    FileUtils.write(propsFile, "sonar.foo=bar");

    ProcessEntryPoint entryPoint = ProcessEntryPoint.createForArguments(new String[]{propsFile.getAbsolutePath()});
    assertThat(entryPoint.getProps().value("sonar.foo")).isEqualTo("bar");
  }

  @Test
  public void test_initial_state() throws Exception {
    Props props = new Props(new Properties());
    ProcessEntryPoint entryPoint = new ProcessEntryPoint(props, exit);

    assertThat(entryPoint.getProps()).isSameAs(props);
    assertThat(entryPoint.isReady()).isFalse();
    assertThat(entryPoint.getState()).isEqualTo(State.INIT);

    // do not fail
    entryPoint.ping();
  }

  @Test
  public void fail_to_launch_if_missing_monitor_properties() throws Exception {
    Props props = new Props(new Properties());
    ProcessEntryPoint entryPoint = new ProcessEntryPoint(props, exit);

    StandardProcess process = new StandardProcess();
    try {
      entryPoint.launch(process);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Missing property: process.key");
      assertThat(process.getState()).isEqualTo(State.INIT);
    }
  }

  @Test
  public void fail_to_launch_multiple_times() throws Exception {
    Props props = new Props(new Properties());
    props.set(ProcessEntryPoint.PROPERTY_PROCESS_KEY, "test");
    props.set(ProcessEntryPoint.PROPERTY_AUTOKILL_DISABLED, "true");
    props.set(ProcessEntryPoint.PROPERTY_TERMINATION_TIMEOUT, "30000");
    ProcessEntryPoint entryPoint = new ProcessEntryPoint(props, exit);

    entryPoint.launch(new NoopProcess());
    try {
      entryPoint.launch(new NoopProcess());
      fail();
    } catch (IllegalStateException e) {
      assertThat(e).hasMessage("Already started");
    }
  }

  @Test
  public void launch_then_request_graceful_termination() throws Exception {
    Props props = new Props(new Properties());
    props.set(ProcessEntryPoint.PROPERTY_PROCESS_KEY, "test");
    props.set(ProcessEntryPoint.PROPERTY_AUTOKILL_DISABLED, "true");
    props.set(ProcessEntryPoint.PROPERTY_TERMINATION_TIMEOUT, "30000");
    final ProcessEntryPoint entryPoint = new ProcessEntryPoint(props, exit);
    final StandardProcess process = new StandardProcess();

    Thread runner = new Thread() {
      @Override
      public void run() {
        // starts and waits until terminated
        entryPoint.launch(process);
      }
    };
    runner.start();

    while (process.getState() != State.STARTED) {
      Thread.sleep(10L);
    }

    // requests for termination -> waits until down
    // Should terminate before the timeout of 30s
    entryPoint.terminate();

    assertThat(process.getState()).isEqualTo(State.STOPPED);
  }

  @Test
  public void autokill_if_no_pings() throws Exception {
    Props props = new Props(new Properties());
    props.set(ProcessEntryPoint.PROPERTY_PROCESS_KEY, "test");
    props.set(ProcessEntryPoint.PROPERTY_TERMINATION_TIMEOUT, "30000");
    props.set(ProcessEntryPoint.PROPERTY_AUTOKILL_PING_INTERVAL, "5");
    props.set(ProcessEntryPoint.PROPERTY_AUTOKILL_PING_TIMEOUT, "1");
    final ProcessEntryPoint entryPoint = new ProcessEntryPoint(props, exit);
    final StandardProcess process = new StandardProcess();

    entryPoint.launch(process);

    assertThat(process.getState()).isEqualTo(State.STOPPED);
  }

  @Test
  public void terminate_if_unexpected_shutdown() throws Exception {
    Props props = new Props(new Properties());
    props.set(ProcessEntryPoint.PROPERTY_PROCESS_KEY, "foo");
    props.set(ProcessEntryPoint.PROPERTY_AUTOKILL_DISABLED, "true");
    props.set(ProcessEntryPoint.PROPERTY_TERMINATION_TIMEOUT, "30000");
    final ProcessEntryPoint entryPoint = new ProcessEntryPoint(props, exit);
    final StandardProcess process = new StandardProcess();

    Thread runner = new Thread() {
      @Override
      public void run() {
        // starts and waits until terminated
        entryPoint.launch(process);
      }
    };
    runner.start();
    while (process.getState() != State.STARTED) {
      Thread.sleep(10L);
    }

    // emulate signal to shutdown process
    entryPoint.getShutdownHook().start();
    while (process.getState() != State.STOPPED) {
      Thread.sleep(10L);
    }
    // exit before test timeout, ok !
  }

  @Test
  public void terminate_if_startup_error() throws Exception {
    Props props = new Props(new Properties());
    props.set(ProcessEntryPoint.PROPERTY_PROCESS_KEY, "foo");
    props.set(ProcessEntryPoint.PROPERTY_AUTOKILL_DISABLED, "true");
    props.set(ProcessEntryPoint.PROPERTY_TERMINATION_TIMEOUT, "30000");
    final ProcessEntryPoint entryPoint = new ProcessEntryPoint(props, exit);
    final MonitoredProcess process = new StartupErrorProcess();

    entryPoint.launch(process);
    assertThat(entryPoint.getState()).isEqualTo(State.STOPPED);
  }

  private static class NoopProcess implements MonitoredProcess {

    @Override
    public void start() {

    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void awaitTermination() {

    }

    @Override
    public void terminate() {

    }
  }

  private static class StartupErrorProcess implements MonitoredProcess {

    @Override
    public void start() {
      throw new IllegalStateException("ERROR");
    }

    @Override
    public boolean isReady() {
      return false;
    }

    @Override
    public void awaitTermination() {

    }

    @Override
    public void terminate() {

    }
  }
}
