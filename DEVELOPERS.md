<pre>
    _____        _        _                _        _      ___  
   |  __ \      | |      | |              | |      | |    |__ \ 
   | |  | | __ _| |_ __ _| |__  _   _  ___| | _____| |_      ) |
   | |  | |/ _` | __/ _` | '_ \| | | |/ __| |/ / _ \ __|    / / 
   | |__| | (_| | || (_| | |_) | |_| | (__|   <  __/ |_    / /_ 
   |_____/ \__,_|\__\__,_|_.__/ \__,_|\___|_|\_\___|\__|  |____|
</pre>

#Overview

This file describes rules for working with databucket code and provides userful information for new contributors.

# Contribution rules

1. During development we are following GitHub flow. In order to provide change or fix in project, one has to:

   1. Create a branch from the repository.
   2. Create, edit, rename, move, or delete files.
   3. Send a pull request from your branch with your proposed changes to kick off a discussion.
   4. Make changes on your branch as needed. Your pull request will update automatically.
   5. Merge the pull request once the branch is ready to be merged.
   6. Tidy up your branches using the delete button in the pull request or on the branches page.

   For more information see [Understanding the GitHub flow](https://guides.github.com/introduction/flow/).

2. Each commit must contain clear and descriptive commit messages.

3. Source code must be properly formatted

    [//]: # (TODO add more info once formatter is defined and added to the code)

4. Source code must be checked according to checkstyle rules

    [//]: # (TODO add more info once formatter is defined and added to the code)

5. Changes must be well-tested. See also **Testing strategy** chapter.

# Setup

# Prerequisites

Installed below dependencies:

- Docker
- java8
- nodeJS

## Docker

Create `databucket_mysql` docker container

```
docker run \
--name databucket_mysql \
-e MYSQL_DATABASE=databucket \
-e MYSQL_USER=databucket \
-e MYSQL_PASSWORD=data1234! \
-e MYSQL_RANDOM_ROOT_PASSWORD=true \
-p 3306:3306 \
--detach \
mysql:8
```

## Useful commands

| command       | what it does|
| ------------- |-------------| 
|`./gradlew clean assemble`| compiles java classes|
|`./gradlew test`| runs all tests |
|`./gradlew buildFrontend`| installs frontend dependencies and builds frontend|
|`./gradlew copyFrontend`|`buildFrontend` and copies frontend file to java resources |
|`export SPRING_PROFILES_ACTIVE=dev && ./gradlew bootRun`| starts app with dev profile |
|`./gradlew clean build`| compiles java classes, builds frontend, runs tests and prepares all-in-one runnable jar |

