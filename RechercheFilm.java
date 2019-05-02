import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


//java -cp ".:sqlite-jdbc-3.27.2.1.jar" Main
public class RechercheFilm {
    private Connection _connection;
    private String[] _keywords = {"titre", "de", "avec", "pays", "en", "avant", "apres"};

    public RechercheFilm(String nomFichierSQLite){
        _connection = null;
        try {
            String url = "jdbc:sqlite:" + nomFichierSQLite;
            _connection = DriverManager.getConnection(url);
            System.out.println("Connection to SQLite has been established.");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void fermeBase(){
        try {
            _connection.close();
        }catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public String retrouve(String requete){
        HashMap<String, String> data = new HashMap<String, String>();
        String word = "";
        String actualKey= "", tKey="";
        requete +=" ";

        for(int i=0; i<requete.length(); i++){

            if(requete.charAt(i)== ' '){
                word.toLowerCase();

                if(Arrays.asList(_keywords).contains(word)){
                    data.put(word, null);
                    actualKey = word;

                }else if(actualKey == ""){
                    data.replace(tKey, data.get(tKey), data.get(tKey) + " " + word);
                    //System.out.println(tKey + " : " + data.get(tKey));
                    tKey="";
                }else if(word== "ou"){

                }else{

                    //System.out.println(actualKey + " : " + word);
                    data.replace(actualKey, null, word);
                    tKey=actualKey;
                    actualKey="";
                }
                word = "";

            }else {
                word += requete.charAt(i);
            }
            
        }

        //print map
        for (Map.Entry<String, String> entry : data.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ". Value: " + entry.getValue());
        }

        return "";
    }


    @SuppressWarnings("unchecked")
    public static <T>T[] splice(final T[] array, int start, final int deleteCount) {
        if (start < 0)
            start += array.length;

        final T[] spliced = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length - deleteCount);
        if (start != 0)
            System.arraycopy(array, 0, spliced, 0, start);

        if (start + deleteCount != array.length)
            System.arraycopy(array, start + deleteCount, spliced, start, array.length - start - deleteCount);

        return spliced;
    }

}