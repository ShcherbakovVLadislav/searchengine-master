# searchengine-master
Финальная работа Skillbox - поисковый движок для сайтов, которые находятся в конфигурационном файле "application.yaml".
# Принцип работы поискового движка:
1. В конфигурационном файле перед запуском приложения задаются адреса сайтов, по которым движок должен осуществлять поиск.
2. Поисковый движок должен самостоятельно обходить все страницы заданных сайтов и индексировать их (создавать так называемый индекс) так, чтобы затем находить наиболее релевантные страницы по любому поисковому запросу.
3. Пользователь присылает запрос через API движка. Запрос — это набор слов, по которым нужно найти страницы сайта.
4. Запрос определённым образом трансформируется в список слов, переведённых в базовую форму. Например, для существительных — именительный падеж, единственное число.
5. В индексе ищутся страницы, на которых встречаются все эти слова.
6. Результаты поиска ранжируются, сортируются и отдаются пользователю.
# Стэк используемых технологий:
Java version 19, Spring Boot version 2.5.7, maven, Hibernate, MySQL, Lombok, JSOUP, Morphology Library.
# Инструкция по локальному запуску проекта:
1. В файле конфигурации "application.yaml" указать логин и пароль от аккаунта MySQL и сайты, которые хотите индексировать.
2. Создать пустую базу данных search_engine.
3. Указать токен для доступа к репозиторию для лемматизации слов. Для указания токена найдите или создайте файл
   settings.xml.
   ● В Windows он располагается в директории
   C:/Users/<Имя вашего пользователя>/.m2
   ● В Linux — в директории
   /home/<Имя вашего пользователя>/.m2
   ● В macOs — по адресу
   /Users/<Имя вашего пользователя>/.m2