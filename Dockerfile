FROM maven:3.9.8-amazoncorretto-21

LABEL authors="dlorant"

# Add your application's source code to the container
ADD . /app

WORKDIR /app

# Compile your application
RUN mvn clean install

# Copy the entrypoint script into the container and make it executable
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

# Set the entrypoint script as the entrypoint
ENTRYPOINT ["/docker-entrypoint.sh"]
# CMD can be used to specify default arguments to ENTRYPOINT if needed