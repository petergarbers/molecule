FROM clojure
RUN mkdir -p /root/.lein
ADD profiles.clj /root/.lein/profiles.clj
RUN mkdir -p /src/app
WORKDIR /src/app
COPY project.clj /src/app
RUN lein deps
