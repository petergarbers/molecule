FROM clojure
RUN mkdir -p /src/app
WORKDIR /src/app
COPY project.clj /src/app
RUN lein deps