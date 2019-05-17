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
        StringBuilder sql_req = new StringBuilder("SELECT t.titre, ln.nom, fn.prenom, c.pays, d.duree, a.titre from films as M, personnes as P, pays as C");
        StringBuilder filter = new StringBuilder("with filtre");
        int valueLength = 0;
        String chars = "";
        String[] array;
        //System.out.println(title_filter);

        for (Map.Entry<String, String> entry : user_req.entrySet()) {
            //System.out.println("Key: " + entry.getKey() + ". Value: " + entry.getValue());
            valueLength = entry.getValue().length();
            
            switch(entry.getKey()){
                case "TITRE":
                    if(!entry.getValue().contains("ou") && !entry.getValue().contains(","))
                        filter.append(" (SELECT id_film FROM recherche_titre WHERE titre MATCH '" + entry.getKey() + "') ");
                    else{
                        if(entry.getValue().contains("ou")){
                            chars = new StringBuilder().append(entry.getValue().charAt(valueLength-3)).append(entry.getValue().charAt(valueLength-2)).append(entry.getValue().charAt(valueLength-1)).toString();
                            if(chars != " ou"){
                                array = get(entry.getValue(), "ou");

                                chars = " (SELECT id_film FROM recherche_titre WHERE titre MATCH '" + array[0] + "' ";
                                for(int i = 1; i<chars.length(); i++){
                                    chars += "UNION SELECT id_film FROM recherche_titre WHERE titre MATCH '" + array[i] + "'";
                                }
                                chars += ") " ;
                                filter.append(chars);
                            }else if(chars == "ou"){
                                filter.append(" UNION "); //au moins un OU en fin de ligne
                            }
                            }else if(entry.getValue().contains(",")){
                                chars = new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString();
                                if(chars == ","){
                                    filter.append(" INTERSECT ");
                                }
                            }
                    }
                    break;

                case "DE":
                    break;

                case "AVEC":
                    break;

                case "PAYS":
                    if(!entry.getValue().contains("ou") && !entry.getValue().contains(",")){
                            filter.append(" (SELECT id_film FROM films, pays where films.pays= pays.code and films.pays='" + entry.getKey() + "' OR pays.nom = '" + entry.getKey() + "') ");
                
                    }else{
                        if(entry.getValue().contains("ou")){
                            chars = new StringBuilder().append(entry.getValue().charAt(valueLength-3)).append(entry.getValue().charAt(valueLength-2)).append(entry.getValue().charAt(valueLength-1)).toString();

                            if(chars != " ou" && entry.getValue().length()>2){
                                array = get(entry.getValue(), "ou");
                                chars = " (SELECT id_film FROM films NATURAL JOIN pays WHERE nom='" + array[0] + "' ";
                                for(int i = 1; i<chars.length(); i++){
                                    chars += "UNION SELECT id_film FROM films NATURAL JOIN pays WHERE nom='" + array[i] + "'";
                                }
                                chars += ") " ;
                                filter.append(chars);
                            }else if(chars != " ou" && entry.getValue().length()<3){
                                array = get(entry.getValue(), "ou");
                                chars = " (SELECT id_film FROM films WHERE pays='" + array[0] + "' ";
                                for(int i = 1; i<chars.length(); i++){
                                    chars += "UNION SELECT id_film FROM films NATURAL JOIN pays WHERE nom='" + array[i] + "'";
                                }
                                chars += ") " ;
                                filter.append(chars);

                            }else if(chars == "ou")
                                filter.append(" UNION "); //au moins un OU en fin de ligne
                            
                        }else if(entry.getValue().contains(",")){
                            chars = new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString();
                            if(chars == ",")
                                filter.append(" INTERSECT ");
                            
                        }
                    }
                    break;

                case "EN":
                    if(!entry.getValue().contains("ou") && !entry.getValue().contains(","))
                        filter.append(" (SELECT id_film FROM films WHERE annee=" + entry.getKey() + ") ");
                    else{
                        if(entry.getValue().contains("ou")){
                            chars = new StringBuilder().append(entry.getValue().charAt(valueLength-3)).append(entry.getValue().charAt(valueLength-2)).append(entry.getValue().charAt(valueLength-1)).toString();
                            if(chars != " ou"){
                                array = get(entry.getValue(), "ou");
                                chars = " (SELECT id_film FROM films WHERE annee=" + array[0];
                                for(int i = 1; i<chars.length(); i++){
                                    chars += "UNION SELECT id_film FROM films WHERE annee=" + array[i];
                                }
                                chars += ") " ;

                                filter.append(chars);
                            }else 
                                filter.append(" UNION "); //au moins un OU en fin de ligne
                        
                        }else if(entry.getValue().contains(",")){
                            chars = new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString();
                            if(chars != ","){
                                //message d'erreur
                            }else 
                                filter.append(" INTERSECT ");
                        }
                    }
                    break;

                case "AVANT":
                    if(!entry.getValue().contains("ou") && !entry.getValue().contains(","))
                        filter.append(" (SELECT id_film FROM films WHERE annee<" + entry.getKey() + ") ");
                    else{
                        if(entry.getValue().contains("ou")){
                            chars = new StringBuilder().append(entry.getValue().charAt(valueLength-3)).append(entry.getValue().charAt(valueLength-2)).append(entry.getValue().charAt(valueLength-1)).toString();
                            if(chars != " ou"){
                                array = get(entry.getValue(), "ou");
                                chars = " (SELECT id_film FROM films WHERE annee<" + array[0];
                                for(int i = 1; i<chars.length(); i++){
                                    chars += "UNION SELECT id_film FROM films WHERE annee<" + array[i];
                                }
                                chars += ") " ;

                            filter.append(chars);

                            }else 
                                filter.append(" UNION "); //au moins un OU en fin de ligne

                        }else if(entry.getValue().contains(",")){
                            chars = new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString();
                            if(chars != ","){
                                //message d'erreur
                            }else 
                                filter.append(" INTERSECT ");
                        }
                    }
                    break;

                case "APRES":
                    if(!entry.getValue().contains("ou") && !entry.getValue().contains(","))
                        filter.append(" (SELECT id_film FROM films WHERE annee>" + entry.getKey() + ") ");
                    else{
                        if(entry.getValue().contains("ou")){
                            chars = new StringBuilder().append(entry.getValue().charAt(valueLength-3)).append(entry.getValue().charAt(valueLength-2)).append(entry.getValue().charAt(valueLength-1)).toString();
                            if(chars != " ou"){
                                array = get(entry.getValue(), "ou");
                                chars = " (SELECT id_film FROM films WHERE annee>" + array[0];
                                for(int i = 1; i<chars.length(); i++){
                                    chars += "UNION SELECT id_film FROM films WHERE annee>" + array[i];
                                }
                                chars += ") " ;

                            filter.append(chars);

                            }else 
                                filter.append(" UNION "); //au moins un OU en fin de ligne
                                
                        }else if(entry.getValue().contains(",")){
                            chars = new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString();
                            if(chars != ","){
                                //message d'erreur
                            }else 
                                filter.append(" INTERSECT ");
                        }
                    }
                    break;
            }

        }

        System.out.println(filter.toString());
        
        return "";
    }

    public String[] get(String line, String type){
        if(type == "ou")
            return line.split("ou");
        else
            return line.split(",");
    }

  
}