# BP21_Quantumnetwork_Controlcenter


Um das Projekt in Eclipse compilen zu können muss für die SQL Datenbank ein Treiber in Form einer externen .jar heruntergeladen werden und dem Projekt als Referenced Library hinzugefügt werden.
Diese Datei heißt sqlite-jdbc-VERSION.jar und kann hier herunter geladen werden: https://github.com/xerial/sqlite-jdbc.

Um das Projekt in Intellij compilen zu können, muss hier für die SQL Datenbank der entsprechende Treiber in Form einer Library hinzugefügt werden. Diese ist in den Plugins verfügbar. Zum Projekt hinzugefügt werden kann sie, in dem man über 'File' das Fenster zu 'Project Structure' öffnen (alternativ die Tastenkombination Strg+Alt+Umschalt+S). Dort findet man den Abschnitt 'Modules' und dort können weitere Jar Dateien hinzugefügt werden. Die benötigte findet sich im Verzeichnis der IDE im Unterordner plugins/svn4idea/ und nennt sich sqlite-jdbc-VERSION.jar . Alternativ kann man es sich auch herunterladen und stattdessen die heruntergeladene Jar Datei als Pfad auswählen.

<b><u>Creating a jar</u></b>

<i>Intellij:</i>  
File -> Project Structure -> Artifacts  
Create a new Aritfact -> JAR -> From modules with dependencies
Module should already be "Quantumnetwork Controllcenter", otherwise choose it  
Select the main class ("QuantumnetworkControllcenter (frame)")  
Leave the selection on "extract to the target JAR"  
choose the correct Directory (should be the one above the "META-INF" folder, currently "Quantumnetwork Controllcenter"  
Make sure the box infront of "Include tests" is not checked  
OK  
Remove all unnecessary libraries from the included parts (left side)  
(Currently, only the sqlite-jdbc-(ver).jar should be necessary, with (ver) replaced with the correct number code)  
Change the name to not have a space (better for file usage)  
OK  
Build -> Build Artifacts  
Select the correct one and choose "Build"  
The Jar is located in the folder Quantumnetwork Controllcenter/out/artifacts/QuantumnetworkControllcenter_jar/  
(File separators might vary between different operating systems)  
(This way includes the META-INF for the jdbc library, including the license)

<i>Eclipse:</i>  
Right-click in the project -> Export -> Runnable jar  
Choose the main class "Quantumnetwork Controllcenter" of the package "frame" for Launch configuration  
Choose an Export destination  
Choose the option "Extract required libraries into the generated JAR"  
Click "Finish"
