javac --module-path /home/$1/ALAANYA_COM-main/lib/libJavaFx --add-modules javafx.controls,javafx.fxml  -cp /home/$1/ALAANYA_COM-main/lib/mysql-connector-j-8.3.0.jar -d bin src/main/java/alaanya/*.java src/main/java/audio/*.java src/main/java/file/*.java src/main/java/message/*.java 
jar --create --file AlaanyaApp.jar --manifest MANIFEST.MF -C bin . -C src/main/resources .

java --module-path /home/$1/ALAANYA_COM-main/lib/libJavaFx --add-modules javafx.controls,javafx.fxml -jar AlaanyaApp.jar

