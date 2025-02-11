package video;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import org.opencv.core.Core;

import static java.lang.System.loadLibrary;


public class VideoPlayer extends Application {

    //Declare la capture vidéo
    private VideoCapture camera;
    private ImageView imageView;
    private Mat frame;

    // Chargement de la bibliothèque native OpenCV
    static {
        loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Override
    public  void start(Stage stage) {
    // Créer un canvas pour dessiner la vidéo
        Canvas canvas = new Canvas(800, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        //Lancer la capture vidéo
        camera = new VideoCapture(0); // 0 pour la webcam

        if (!camera.isOpened()){
            System.out.println("Impossible to open camera");
            return;
        }

        //Créer un fil d'execution pour la capture vidéo
        Thread videoThread = new Thread(() ->{
            frame = new Mat();
            while(camera.read(frame)){
                if (!frame.empty()){
                    //Convertir l'image en format adapté à javafx
                    Mat flippedFrame = new Mat();
                    Imgproc.cvtColor(frame, flippedFrame, Imgproc.COLOR_BGR2RGB);

                    //Convertir en image JavFx
                    Image image = matToImage(flippedFrame);

                    //Afficher l'image dans la fenetre JavaFx
                    javafx.application.Platform.runLater(()->{
                        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                        gc.drawImage(image, 0, 0);

                    });
                }
            }
        });
        videoThread.setDaemon(true);
        videoThread.start();

        //Configurer la scene JavaFx
        StackPane root = new StackPane();
        root.getChildren().add(canvas);
        Scene scene = new Scene(root, 700, 500);
        stage.setScene(scene);
        stage.setTitle("Video Player");
        stage.show();
   }



    //METHODE POUR CONVERTIR UNE mAT EN iMAGE
    private Image matToImage(Mat mat){
       
        //Convertir la Mat en tableau de bytes
        byte[] data = new byte[(int)(mat.total()*mat.channels())];
        mat.get(0, 0, data);

        //Extrait les données de la matrice
        WritableImage writableImage = new WritableImage(mat.width(), mat.height());
        PixelWriter pixelWriter = writableImage.getPixelWriter();
        for (int i = 0; i <mat.height();i++){
            for (int j = 0; j <mat.width();j++){
                //Recuperer les valeurs de chaque pixel
                int r = data[(i* mat.width()+j)*3]; //Canal rouge
                int g = data[(i* mat.width()+j)*3+1]; //Canal vert
                int b = data[(i* mat.width()+j)*3+2]; //Canal bleu

                //S'assurer que les valeurs des pixels sont dans la plage (0;255)
                r = Math.min(255,Math.max(0,r));
                g = Math.min(255,Math.max(0,g));
                b = Math.min(255,Math.max(0,b));

                //Definir la couleur du pixel dans l'image javafX

                pixelWriter.setColor(j,i,javafx.scene.paint.Color.rgb(r,g,b));
            }
        }
        return writableImage;
    }

    @Override
    public void stop() {
        //Libérer la camera et les ressources
        if (camera != null){
            camera.release();
        }
    }


    public static void main(String[] args) {
        launch(args); // Lancement de l'application JavaFX
    }
}
