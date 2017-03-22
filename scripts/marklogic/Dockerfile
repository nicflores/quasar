FROM patrickmcelwee/marklogic-dependencies:8-latest

# Install MarkLogic
WORKDIR /tmp
ADD MarkLogic.rpm /tmp/MarkLogic.rpm

RUN yum -y install /tmp/MarkLogic.rpm

# Expose MarkLogic Server ports - plus 8040, 8041, 8042 for your REST, etc
# endpoints - feel free to add more
EXPOSE 7997 7998 7999 8000 8001 8002 8040 8041 8042

# Define default command (which avoids immediate shutdown)
CMD /opt/MarkLogic/bin/MarkLogic && tail -f /dev/null
