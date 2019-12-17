# TodoTelegramBot
[![Language](http://img.shields.io/badge/language-java-brightgreen.svg)](https://www.java.com/)
[![License](http://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/samtools/PolinaBevad/bio_relatives)

Course project on the subject of Software Design of Peter the Great St. Petersburg Polytechnic University students

## Table of Contents
-   [Relevance](#Actuality)
-   [Features list](#features-list)
-   [Communication with the Telegram server](#communication-with-the-telegram-server)
-   [Usage](#Usage)
    -   [How to find this bot](#how-to-find-this-bot)
    -   [Buttons and functionality](#buttons-and-functionality)
-   [Testing](#Testing)
    -   [Test coverage](#test-coverage)
-   [Deployment](#Deployment)
-   [Maintainers](#Maintainers)
-   [License](#License)

## Relevance
The modern rhythm of life requires a large number of tasks. In order not to miss anything, you need to store your tasks in some form. And there is nothing more convenient than to manage your tasks for the day with the help of a Telegram bot.

## Features list
This bot can:
-   Create tasks
-   Change status of the tasks
-   Find not completed tasks
-   Find all tasks
-   Find tasks by date
-   Notify about tasks for the day in the morning

## Communication with the Telegram server
Communication between the backend and the Telegram is carried out by sending requests to the Telegram API by a unique token.
To be able to do this, we use [TelegramBots](https://github.com/rubenlagus/TelegramBots) library.


## Usage
### How to find this bot
You can find this bot in Telegram by it's name - `@TheBestToDoBot`
 
### Buttons and functionality
-   `Show help` - click this button to get help information.
-   `Show all tasks` - click this button to get all your tasks.
-   `Create new task` - click this button to create a new task. You should send information, which Bot needs: due date, task description and priority.
-   `Mark task as completed` - click this button to mark any task as completed.
-   `Find tasks by due date` - click this button to get all the tasks by some specific due date.
-   `Find not completed tasks` - click this button to get the list of all not completed tasks.

Also, our bot is able to send you notification messages every morning at 8 am.
    
## Testing
Unit tests were conducted using [Mockito](https://site.mockito.org/) and [JUnit](https://junit.org/junit5/).
### Test coverage
Сode coverage with tests was evaluated using [JaCoCo](https://www.jacoco.org/jacoco/).

<img src="/src/main/resources/static/test_coverage.jpg" width=auto height=auto />

## Deployment
This bot was deployed to [Heroku](https://www.heroku.com) and uses the free version of PostgreSQL database to store data about todos, which is provided by Heroku by default. 
## Maintainers
Students of 3530904/70103 group:
-   Sergey Khvatov
-   Vladislav Marchenko
-   Kamil Kadyrov
## License
This project is licenced under the terms of the [MIT](LICENSE) license.
