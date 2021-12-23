# BP21_Quantumnetwork_Controlcenter


Um das Projekt in Eclipse compilen zu können muss für die SQL Datenbank ein Treiber in Form einer externen .jar heruntergeladen werden und dem Projekt als Referenced Library hinzugefügt werden.
Diese Datei heißt sqlite-jdbc-VERSION.jar und kann hier herunter geladen werden: https://github.com/xerial/sqlite-jdbc.

Um das Projekt in Intellij compilen zu können, muss hier für die SQL Datenbank der entsprechende Treiber in Form einer Library hinzugefügt werden. Diese ist in den Plugins verfügbar. Zum Projekt hinzugefügt werden kann sie, in dem man über 'File' das Fenster zu 'Project Structure' öffnen (alternativ die Tastenkombination Strg+Alt+Umschalt+S). Dort findet man den Abschnitt 'Modules' und dort können weitere Jar Dateien hinzugefügt werden. Die benötigte findet sich im Verzeichnis der IDE im Unterordner plugins/svn4idea/ und nennt sich sqlite-jdbc-VERSION.jar . Alternativ kann man es sich auch herunterladen und stattdessen die heruntergeladene Jar Datei als Pfad auswählen.
