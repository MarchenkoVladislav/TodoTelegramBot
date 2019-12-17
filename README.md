# TodoTelegramBot
[![Language](http://img.shields.io/badge/language-java-brightgreen.svg)](https://www.java.com/)
[![License](http://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/samtools/PolinaBevad/bio_relatives)

Course project on the subject of Software Design of Peter the Great St. Petersburg Polytechnic University students

## Table of Contents
-   [Actuality](#Actuality)
-   [Features list](#features-list)
-   [Communication with the Telegram server](#communication-with-the-telegram-server)
-   [Usage](#Usage)
    -   [Bot name(how to find this bot)](#bot-namehow-to-find-this-bot)
    -   [Buttons and functionality](#buttons-and-functionality)
-   [Testing](#Testing)
    -   [Test coverage](#test-coverage)
-   [Deploying](#Deploying)
-   [Maintainers](#Maintainers)
-   [License](#License)

## Actuality
The modern rhythm of life requires a large number of tasks. In order not to miss anything, you need to store your tasks in some form. And there is nothing more convenient than to manage your tasks for the day with the help of a Telegram bot.

## Features list
This bot can:
-   Create tasks
-   Change statuses of tasks
-   Find not completed tasks
-   Find all tasks
-   Find tasks by date
-   Notify about tasks for the day in the morning

## Communication with the Telegram server
Communication between the backend and the Telegram is carried out by sending requests to the Telegram API by a unique token.


## Usage
### Bot name(how to find this bot)
You can find this bot in Telegram by bot name `@TheBestToDoBot`
 
### Buttons and functionality
-   `Show help` - click this button to get help information.
-   `Show all tasks` - click this button to get all own tasks.
-   `Create new task` - click this button to start creation of new task. After that you should send information, which the Bot need: due date, task info and task priority.
-   `Mark task as completed` - click this button to mark any task completed. Also you should to send an id of necessary task.
-   `Find tasks by due date` - click this button to get tasks by due date. Also you should to send a necessary date.
-   `Find not completed tasks` - click this button to get a list of not completed tasks.
    
## Testing
Unit tests were conducted using [Mockito](#site.mockito.org/) and [JUnit](#junit.org/junit5/).
### Test coverage
Сode coverage by tests was evaluated with [JaCoCo](#www.jacoco.org/jacoco/).

<img src="/src/main/resources/static/test_coverage.jpg" width=auto height=auto/>

## Deploying
This bot was deployed on [Heroku](#heroku.com)
## Maintainers
Students of 3530904/70103 group:
-   Sergey Khvatov
-   Vladislav Marchenko
-   Kamil Kadyrov
## License
This project is licenced under the terms of the [MIT](LICENSE) license.
