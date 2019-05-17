import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/***
 * 
 */

//java -cp ".:sqlite-jdbc-3.27.2.1.jar" Main
public class RechercheFilm {
    private Connection _connection;
    private String[] _keywords = {"TITRE", "DE", "AVEC", "PAYS", "EN", "AVANT", "APRES"};

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
        LinkedHashMap<String, String> data = new LinkedHashMap<String, String>();
        String word = "";
        String actualKey= "", tKey="";
        requete +=" ";
       
        for(int i=0; i<requete.length(); i++){

            if(requete.charAt(i)== ' '){
                word = word.toUpperCase();

                if(Arrays.asList(_keywords).contains(word)){
                    if(!data.containsKey(word))
                        data.put(word, null);
                    actualKey = word;

                }else if(actualKey == ""){
                    word = word.toLowerCase();
                    data.replace(tKey, data.get(tKey), data.get(tKey) + " " + word);
                    tKey="";

                }else if(word == "ou"){
                   data.replace(tKey, data.get(tKey), data.get(tKey) + " " + word);
                   actualKey = "";
                  

                }else{
                    word = word.toLowerCase();
                    if(data.get(actualKey) != null)
                        data.replace(actualKey, data.get(actualKey), data.get(actualKey) + " " + word);
                    else
                        data.replace(actualKey, null, word);
                    tKey=actualKey;
                }
                word = "";

            }else 
                word += requete.charAt(i);
            
            
        }


        return analyzeRequest(data);
    }




    public String analyzeRequest(LinkedHashMap<String, String> user_req){
        StringBuilder sql_req = new StringBuilder();
        for (Map.Entry<String, String> entry : user_req.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ". Value: " + entry.getValue());
            switch(entry.getKey()){
                case "TITRE":
                    break;
                case "DE":
                    break;
                case "AVEC":
                    break;
                case "PAYS":
                    break;
                case "EN":
                    break;
                case "AVANT":
                    break;
                case "APRES":
                    break;
            }
        }
        
        return "";
    }
}