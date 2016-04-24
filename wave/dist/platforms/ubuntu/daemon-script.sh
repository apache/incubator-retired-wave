#!/bin/sh

#     Licensed to the Apache Software Foundation (ASF) under one
#     or more contributor license agreements.  See the NOTICE file
#     distributed with this work for additional information
#     regarding copyright ownership.  The ASF licenses this file
#     to you under the Apache License, Version 2.0 (the
#     "License"); you may not use this file except in compliance
#     with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
#     Unless required by applicable law or agreed to in writing,
#     software distributed under the License is distributed on an
#     "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#     KIND, either express or implied.  See the License for the
#     specific language governing permissions and limitations
#     under the License.

### BEGIN INIT INFO
# Provides:             wave
# Required-Start:       $network $local_fs $remote_fs
# Required-Stop:
# Default-Start:        3 4 5
# Default-Stop:         S 0 1 2 6
# Short-Description:    Wave in a Box Collaboration Server
### END INIT INFO


# Attempt to locate JAVA_HOME, code borrowed from jabref package
if [ -z $JAVA_HOME ]
then
        t=/usr/lib/jvm/java-1.5.0-sun && test -d $t && JAVA_HOME=$t
        t=/usr/lib/jvm/java-6-sun && test -d $t && JAVA_HOME=$t
fi

PATH=/sbin:/bin:/usr/sbin:/usr/bin:${JAVA_HOME}/bin
JAVA=${JAVA_HOME}/bin/java
NAME=wave
DESC=wave
WAVE_HOME=/var/wave

test -x $JAVA || exit 0

export WAVE_HOME

#Helper functions
start() {
        start-stop-daemon --start --quiet --background --make-pidfile \
                --pidfile /var/run/$NAME.pid --chuid wave:wave \
                --exec ${WAVE_HOME}/server-wrapper.sh
}

stop() {
        start-stop-daemon --stop --quiet --pidfile /var/run/$NAME.pid \
                --retry 4
}

case "$1" in
  start)
        echo -n "Starting $DESC: "
        start
        echo "$NAME."
        ;;
  stop)
        echo -n "Stopping $DESC: "
        stop
        echo "$NAME."
        ;;
  restart)
        echo -n "Restarting $DESC: "
        stop
        sleep 1
        start

        echo "$NAME."
        ;;
  *)
        N=/etc/init.d/$NAME
        echo "Usage: $N {start|stop|restart}" >&2
        exit 1
        ;;
esac

exit 0
