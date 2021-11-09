<pre>
    ██████╗  █████╗ ████████╗ █████╗ ██████╗ ██╗   ██╗ ██████╗██╗  ██╗███████╗████████╗    ██████╗
    ██╔══██╗██╔══██╗╚══██╔══╝██╔══██╗██╔══██╗██║   ██║██╔════╝██║ ██╔╝██╔════╝╚══██╔══╝    ╚════██╗
    ██║  ██║███████║   ██║   ███████║██████╔╝██║   ██║██║     █████╔╝ █████╗     ██║        █████╔╝
    ██║  ██║██╔══██║   ██║   ██╔══██║██╔══██╗██║   ██║██║     ██╔═██╗ ██╔══╝     ██║        ╚═══██╗
    ██████╔╝██║  ██║   ██║   ██║  ██║██████╔╝╚██████╔╝╚██████╗██║  ██╗███████╗   ██║       ██████╔╝
    ╚═════╝ ╚═╝  ╚═╝   ╚═╝   ╚═╝  ╚═╝╚═════╝  ╚═════╝  ╚═════╝╚═╝  ╚═╝╚══════╝   ╚═╝       ╚═════╝
</pre>

# About project

**Databucket** is an Open Source Test Data Management Tool. It delivers features to effectively create and maintain test data and metadata for extensive tests on multiple projects, environments, and various integrated tools at the same time. Databucket stores data in elastic structures, which gives a ready approach to maintain constantly changing test data in the software development process.

# Getting started with Databucket
* First you can read about key features on [databucket.pl](https://databucket.pl)
* Next, you can dive deeper in the [Wiki page](https://github.com/databucket/databucket-server/wiki).

# Contribution rules

1. During development, we are following GitHub flow. In order to provide change or fix in project, one has to:

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

- java8
- nodeJS

## Useful commands

| command       | what it does|
| ------------- |-------------| 
|`./gradlew clean assemble`| compiles java classes|
|`./gradlew test`| runs all tests |
|`./gradlew buildFrontend`| installs frontend dependencies and builds frontend|
|`./gradlew copyFrontend`|`buildFrontend` and copies frontend file to java resources |
|`export SPRING_PROFILES_ACTIVE=dev && ./gradlew bootRun`| starts app with dev profile |
|`./gradlew clean build`| compiles java classes, builds frontend, runs tests and prepares all-in-one runnable jar |

