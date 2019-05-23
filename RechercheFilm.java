import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;


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
                    data.replace(tKey, data.get(tKey), data.get(tKey) + " " + word.replaceAll("\\s+", ""));
                    tKey="";

                }else if(word == "ou"){
                   data.replace(tKey, data.get(tKey), data.get(tKey) + " " + word.replaceAll("\\s+", ""));
                   actualKey = "";
                  

                }else{
                    word = word.toLowerCase();
                    if(data.get(actualKey) != null)
                        data.replace(actualKey, data.get(actualKey), data.get(actualKey) + " " + word.replaceAll("\\s+", ""));
                    else
                        data.replace(actualKey, null, word.replaceAll("\\s+", ""));
                    tKey=actualKey;
                }
                word = "";
            }else 
                word += requete.charAt(i);
        }
        return analyzeRequest(data);
    }




    public String analyzeRequest(LinkedHashMap<String, String> user_req){
        StringBuilder sql_req = new StringBuilder("select F.titre,F.annee, F.duree, group_concat(A.titre, '|') as autres_titres,P.prenom, P.nom, G.role from filtre join films f on F.id_film = filtre.id_film join pays py on Py.code = F.pays left join autres_titres a on A.id_film = F.id_film join generique g on G.id_film = F.id_film join personnes p on P.id_personne = G.id_personne group by F.titre,F.annee, F.duree, P.prenom, P.nom, G.role");
        StringBuilder filter = new StringBuilder("with filtre as");
        ArrayList<String> names = new ArrayList<String>();
        String first = "", last= "", and="", or="";
        int valueLength = 0, number = 0;
        String chars = "(", type="";
        String[] array;
        ArrayList<String> errors = new ArrayList<String>();

        for (Map.Entry<String, String> entry : user_req.entrySet()) {
            valueLength = entry.getValue().length();
            switch(entry.getKey()){
                case "TITRE":
                    if((new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString()).equals(",") && (entry.getValue().length() - entry.getValue().replace(",", "").length()) == 1)
                        chars += " SELECT id_film FROM recherche_titre WHERE titre MATCH '" + new StringBuilder(entry.getValue()).deleteCharAt(valueLength-1).toString() + "' ";
                    if(!entry.getValue().contains("ou") && !entry.getValue().contains(","))
                        chars += " SELECT id_film FROM recherche_titre WHERE titre MATCH '" + entry.getValue() + "' ";
                    else{ 
                        if(entry.getValue().contains("ou")){
                            type = new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString();
                            array = (type.equals(",")) ? get((new StringBuilder(entry.getValue()).deleteCharAt(valueLength-1).toString()).toString(), "ou") : get(entry.getValue(), "ou");
                            
                            for(int i = 0; i<array.length; i++){
                                chars += (i==0) ? " SELECT id_film FROM recherche_titre WHERE titre MATCH '" + array[i] + "' " : "UNION SELECT id_film FROM recherche_titre WHERE titre MATCH '" + array[i] + "'";
                            }

                            type = new StringBuilder().append(entry.getValue().charAt(valueLength-3)).append(entry.getValue().charAt(valueLength-2)).append(entry.getValue().charAt(valueLength-1)).toString();
                            if(type.equals(" ou"))
                                chars += " UNION "; 
                        } if(entry.getValue().contains(",")){
                            type = new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString();
                        
                            if(type.equals(",") && (entry.getValue().length() - entry.getValue().replace(",", "").length()) == 1)
                                chars += " INTERSECT "; 
                            else
                                errors.add("Et inclusif sur deux titres");    
                        }
                    }break;

                case "DE":
                        and = new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString();
                        or = new StringBuilder().append(entry.getValue().charAt(valueLength-3)).append(entry.getValue().charAt(valueLength-2)).append(entry.getValue().charAt(valueLength-1)).toString();
                        if(and != ","|| or!= " ou")
                            names = cleanArrayList(constructNameReq(entry.getValue()+","));
                        else
                            names = cleanArrayList(constructNameReq(entry.getValue()));

                        first = names.get(0);
                        for(int i=0; i<names.size(); i++){

                            if(names.get(i).equals(",") || names.get(i).equals("ou") || i == names.size()-1){
                                last = names.get(i-1);
                                chars += "SELECT nom, prenom FROM personnes join generique on generique.id_personne = personnes.id_personne where (nom LIKE '" + first + "%' AND nom LIKE '%"+ last + "') OR (prenom LIKE '" + first +"%' AND nom LIKE '%"+ last + "')and generique.role='R'";
                                
                                if(names.get(i).equals(","))
                                    chars += " INTERSECT ";
                                if(names.get(i).equals("ou"))
                                    chars += " UNION ";  
                                if(i != names.size()-1)
                                    first = names.get(i+1);
                                number++;
                            }else
                                number=0;
                            last="";
                              
                        }break;

                case "AVEC":
                    names = constructNameReq(entry.getValue());
                    or = new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString();
                    and = new StringBuilder().append(entry.getValue().charAt(valueLength-3)).append(entry.getValue().charAt(valueLength-2)).append(entry.getValue().charAt(valueLength-1)).toString();
                    if(and != ","|| or!= " ou")
                        names = cleanArrayList(constructNameReq(entry.getValue()+","));
                    else
                        names = cleanArrayList(constructNameReq(entry.getValue()));
                    for(int i=0; i<names.size(); i++){

                        if(names.get(i).equals(",") || names.get(i).equals("ou") || i == names.size()-1){
                            last = names.get(i-1);
                            chars += "SELECT id_film FROM personnes join generique on generique.id_personne = personnes.id_personne where (nom LIKE '" + first + "%' AND nom LIKE '%"+ last + "') OR (prenom LIKE '" + first +"%' AND nom LIKE '%"+ last + "')and generique.role='A'";
                            if(names.get(i).equals(","))
                                chars += " INTERSECT ";
                            if(names.get(i).equals("ou"))
                                chars += " UNION ";  
                            if(i != names.size()-1)
                                first = names.get(i+1);
                            number++;
                        }else
                            number=0;
                        last="";
                        
                    }break;

                case "PAYS":
                    if((new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString()).equals(",") && (entry.getValue().length() - entry.getValue().replace(",", "").length()) == 1)
                        chars += " SELECT id_film FROM films, pays where films.pays= pays.code and films.pays='"+ new StringBuilder(entry.getValue()).deleteCharAt(valueLength-1).toString() + "' OR pays.nom = '"+ new StringBuilder(entry.getValue()).deleteCharAt(valueLength-1).toString() + "' ";
                    if(!entry.getValue().contains("ou") && !entry.getValue().contains(","))
                        chars += " SELECT id_film FROM films, pays where films.pays= pays.code and films.pays='" + entry.getValue() + "' OR pays.nom = '" + entry.getValue() + "' ";
                    else{
                        if(entry.getValue().contains("ou")){
                            type = new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString();
                            array = (type.equals(",")) ? get((new StringBuilder(entry.getValue()).deleteCharAt(valueLength-1).toString()).toString(), "ou") : get(entry.getValue(), "ou");
                            
                            for(int i = 0; i<array.length; i++){
                                chars += (i==0) ? " SELECT id_film FROM films, pays where films.pays= pays.code and films.pays='" + array[i] +  "' OR pays.nom = '" + array[i] + "' "  : "UNION SELECT id_film FROM films, pays where films.pays= pays.code and films.pays='" + array[i] + "' OR pays.nom = '" + array[i] + "'";
                            }

                            type = new StringBuilder().append(entry.getValue().charAt(valueLength-3)).append(entry.getValue().charAt(valueLength-2)).append(entry.getValue().charAt(valueLength-1)).toString();
                            if(type.equals(" ou"))
                                    chars += " UNION "; 

                        }if(entry.getValue().contains(",")){
                            type = new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString();
                        
                            if(type.equals(",") && (entry.getValue().length() - entry.getValue().replace(",", "").length()) == 1)
                                chars += " INTERSECT "; 
                            else
                                errors.add("Et inclusif sur deux pays");
                                
                        }
                    }break;

                case "EN":
                    if((new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString()).equals(",") && (entry.getValue().length() - entry.getValue().replace(",", "").length()) == 1)
                        chars += " SELECT id_film FROM films WHERE annee=" + new StringBuilder(entry.getValue()).deleteCharAt(valueLength-1).toString() + " ";
                    if(!entry.getValue().contains("ou") && !entry.getValue().contains(","))
                        chars += " SELECT id_film FROM films WHERE annee=" + entry.getValue() + " ";
                    else{
                        if(entry.getValue().contains("ou")){
                            type = new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString();

                            array = (type.equals(",")) ? get((new StringBuilder(entry.getValue()).deleteCharAt(valueLength-1).toString()).toString(), "ou") : get(entry.getValue(), "ou");
                            
                            for(int i = 0; i<array.length; i++){
                                chars += (i==0) ? " SELECT id_film FROM films WHERE annee=" + array[i] + " "  : "UNION SELECT id_film FROM films WHERE annee=" + array[i] + "";
                            }

                            type = new StringBuilder().append(entry.getValue().charAt(valueLength-3)).append(entry.getValue().charAt(valueLength-2)).append(entry.getValue().charAt(valueLength-1)).toString();
                            if(type.equals(" ou"))
                                    chars += " UNION "; 

                        }if(entry.getValue().contains(",")){

                            type = new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString();
                            if(type.equals(",") && (entry.getValue().length() - entry.getValue().replace(",", "").length()) == 1)
                                chars += " INTERSECT ";  
                            else 
                                errors.add("Et inclusif sur deux dates");
                                
                        }
                    }break;

                case "AVANT":
                    if((new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString()).equals(",") && (entry.getValue().length() - entry.getValue().replace(",", "").length()) == 1)
                        chars += " SELECT id_film FROM films WHERE annee<" + new StringBuilder(entry.getValue()).deleteCharAt(valueLength-1).toString() + " ";
                    if(!entry.getValue().contains("ou") && !entry.getValue().contains(","))
                        chars += " SELECT id_film FROM films WHERE annee<" + entry.getValue() + " ";
                    else{
                        if(entry.getValue().contains("ou")){
                            type = new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString();

                            array = (type.equals(",")) ? get((new StringBuilder(entry.getValue()).deleteCharAt(valueLength-1).toString()).toString(), "ou") : get(entry.getValue(), "ou");
                            
                            for(int i = 0; i<array.length; i++){
                                chars += (i==0) ? " SELECT id_film FROM films WHERE annee<" + array[i] + " "  : "UNION SELECT id_film FROM films WHERE annee<" + array[i] + "";
                            }

                            type = new StringBuilder().append(entry.getValue().charAt(valueLength-3)).append(entry.getValue().charAt(valueLength-2)).append(entry.getValue().charAt(valueLength-1)).toString();
                            if(type.equals(" ou"))
                                    chars += " UNION "; 

                        }if(entry.getValue().contains(",")){

                            if(!type.equals(",") && (entry.getValue().length() - entry.getValue().replace(",", "").length()) >= 1){
                                type = new StringBuilder().append(entry.getValue().charAt(valueLength-3)).append(entry.getValue().charAt(valueLength-2)).append(entry.getValue().charAt(valueLength-1)).toString();
                                array = (type.equals(" ou")) ? get((new StringBuilder(entry.getValue()).delete(valueLength-3, valueLength).toString()).toString(), ",") : get(entry.getValue(), ",");

                                for(int i = 0; i<array.length; i++){
                                    chars += (i==0) ? " SELECT id_film FROM films WHERE annee<" + array[i] + " "  : " INTERSECT SELECT id_film FROM films WHERE annee<" + array[i] + "";
                                }
                            }
                        }
                    }break;

                case "APRES": 
                    if((new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString()).equals(",") && (entry.getValue().length() - entry.getValue().replace(",", "").length()) == 1)
                        chars += " SELECT id_film FROM films WHERE annee>" + new StringBuilder(entry.getValue()).deleteCharAt(valueLength-1).toString() + " ";
                    if(!entry.getValue().contains("ou") && !entry.getValue().contains(","))
                        chars += " SELECT id_film FROM films WHERE annee>" + entry.getValue() + " ";
                    else{
                        if(entry.getValue().contains("ou")){
                            type = new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString();
                            array = (type.equals(",")) ? get((new StringBuilder(entry.getValue()).deleteCharAt(valueLength-1).toString()).toString(), "ou") : get(entry.getValue(), "ou");
                            
                            for(int i = 0; i<array.length; i++){
                                chars += (i==0) ? " SELECT id_film FROM films WHERE annee>" + array[i] + " "  : "UNION SELECT id_film FROM films WHERE annee>" + array[i] + "";
                            }

                            type = new StringBuilder().append(entry.getValue().charAt(valueLength-3)).append(entry.getValue().charAt(valueLength-2)).append(entry.getValue().charAt(valueLength-1)).toString();
                            if(type.equals(" ou"))
                                    chars += " UNION "; 

                        }if(entry.getValue().contains(",")){

                            if( (entry.getValue().length() - entry.getValue().replace(",", "").length()) >= 1){
                                type = new StringBuilder().append(entry.getValue().charAt(valueLength-3)).append(entry.getValue().charAt(valueLength-2)).append(entry.getValue().charAt(valueLength-1)).toString();
                                array = (type.equals(" ou")) ? get((new StringBuilder(entry.getValue()).delete(valueLength-3, valueLength).toString()).toString(), ",") : get(entry.getValue(), ",");

                                for(int i = 0; i<array.length; i++){
                                    chars += (i==0) ? " SELECT id_film FROM films WHERE annee>" + array[i] + " "  : "INTERSECT SELECT id_film FROM films WHERE annee>" + array[i] + " ";
                                }

                            }
                           
                        }
                    }break;
  
                    default: 
                        if(new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString().equals(","))
                            chars+= " INTERSECT ";
                        else if(new StringBuilder().append(entry.getValue().charAt(valueLength-3)).append(entry.getValue().charAt(valueLength-2)).append(entry.getValue().charAt(valueLength-1)).toString().equals(" ou"))
                            chars += " UNION ";
                        else //on met une intersection par défaut 
                            chars += " INTERSECT ";
            }

        }
        chars += ") " ;
        filter.append(chars);
        System.out.println("\n\n\n" + filter.toString());
        
        return "";
    }

    public String[] get(String line, String type){
        String[] val = (type == "ou") ? line.split("ou") : line.split(",");   
        return val;
    }
   
    public ArrayList<String> constructNameReq(String req){ 
        StringTokenizer st;
        int i=0;
        String word = "", line = "";
        ArrayList<String> names = new ArrayList<String>();
        
        while ((line = req) != null) {
            st = new StringTokenizer(line, ", ", true); //ou
            while (st.hasMoreTokens()) {
                word = st.nextToken().toLowerCase();
                names.add(i, word);
                i++;
            }
            req=null;
        }
        
        return names;

    }

    public ArrayList<String> cleanArrayList(ArrayList<String> list){
        Iterator<String> it = list.iterator();
            while (it.hasNext())
            {
                if (it.next().equals(" "))
                    it.remove();
                
            }
            return list;
    }

}