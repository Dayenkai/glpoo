import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

//java -cp ".:sqlite-jdbc-3.27.2.1.jar" Main
public class RechercheFilm {
    private Connection _connection;
    private String[] _keywords = {"TITRE", "DE", "AVEC", "PAYS", "EN", "AVANT", "APRES"};

    public RechercheFilm(String nomFichierSQLite){
        _connection = null;
        try {
            String url = "jdbc:sqlite:" + nomFichierSQLite;
            _connection = DriverManager.getConnection(url);
            System.out.println("Connection to SQLite has been established.\n");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    
    public void fermeBase(){
        try {
            _connection.close();
            System.out.println("The database has been closed.\n");
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
            if(requete.trim().length() == 0)
                return "{\"errors\": [ne contient que des espaces] }";
            if(requete.charAt(i) == ' '){
                word = word.toUpperCase();

                if(Arrays.asList(_keywords).contains(word)){
                    if(!data.containsKey(word))
                        data.put(word, null);
                    actualKey = word;

                }else if(actualKey.equals("")){
                    word = word.toLowerCase();
                    data.replace(tKey, data.get(tKey), data.get(tKey) + " " + word.replaceAll("\\s+", ""));
                    tKey="";

                }else if(word.equals("ou")){
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
        StringBuilder sql_req = new StringBuilder("select F.titre,F.annee, F.duree, group_concat(A.titre, '|') as autres_titres,P.prenom, P.nom, G.role, Py.nom as country from filtre\n" +
        "                                                                                                              join films f on F.id_film = filtre.id_film\n" +
        "                                                                                                              join pays py on Py.code = F.pays\n" +
        "                                                                                                              left join autres_titres a on A.id_film = F.id_film\n" +
        "                                                                                                              join generique g on G.id_film = F.id_film\n" +
        "                                                                                                              join personnes p on P.id_personne = G.id_personne\n" +
        "group by F.titre,F.annee, F.duree, P.prenom, P.nom, G.role, F.pays "); //LIMIT 100
        StringBuilder filter = new StringBuilder("with filtre as");
        ArrayList<String> names = new ArrayList<String>();
        String first = "", last= "", and="", or="", dataName="";
        int valueLength = 0, number = 0;
        String chars = "(", type="";
        String[] array;
        ArrayList<String> errors = new ArrayList<String>();

        for (Map.Entry<String, String> entry : user_req.entrySet()) {
            valueLength = entry.getValue().length();
            switch(entry.getKey()){
                case "TITRE":
                    if((new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString()).equals(",") && (entry.getValue().length() - entry.getValue().replace(",", "").length()) == 1)
                        chars += " SELECT id_film FROM recherche_titre WHERE titre MATCH \"" + new StringBuilder(entry.getValue()).deleteCharAt(valueLength-1).toString() + "%\" ";
                    if(!entry.getValue().contains("ou") && !entry.getValue().contains(","))
                        chars += " SELECT id_film FROM recherche_titre WHERE titre MATCH \"" + entry.getValue() + "%\" ";
                    else{ 
                        if(entry.getValue().contains("ou")){
                            type = new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString();
                            array = (type.equals(",")) ? get((new StringBuilder(entry.getValue()).deleteCharAt(valueLength-1).toString()).toString(), "ou") : get(entry.getValue(), "ou");
                            
                            for(int i = 0; i<array.length; i++){
                                chars += (i==0) ? " SELECT id_film FROM recherche_titre WHERE titre MATCH \"" + array[i] + "\" " : "UNION SELECT id_film FROM recherche_titre WHERE titre MATCH \"" + array[i] + "\"";
                            }

                            type = new StringBuilder().append(entry.getValue().charAt(valueLength-3)).append(entry.getValue().charAt(valueLength-2)).append(entry.getValue().charAt(valueLength-1)).toString();
                            if(type.equals(" ou"))
                                chars += " UNION "; 
                        } if(entry.getValue().contains(",")){
                            type = new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString();
                        
                            if(type.equals(",") && (entry.getValue().length() - entry.getValue().replace(",", "").length()) == 1)
                                chars += " INTERSECT "; 
                            else
                                errors.add("et entre deux titres");    
                        }
                    }break;

                case "DE":
                        dataName = entry.getValue(); 
                        if(new StringBuilder().append(dataName.charAt(0)).append(dataName.charAt(1)).toString() == "mc")
                            dataName = new StringBuilder(dataName).insert(1, "a").toString();
                        and = new StringBuilder().append(dataName.charAt(valueLength-1)).toString();
                        or = new StringBuilder().append(dataName.charAt(valueLength-3)).append(dataName.charAt(valueLength-2)).append(dataName.charAt(valueLength-1)).toString();

                        if(!and.equals(",") && !or.equals(" ou"))
                            names = cleanArrayList(constructNameReq(dataName + " @"));
                        else
                            names = cleanArrayList(constructNameReq(dataName));

                        first = names.get(0); 
                        
                        for(int i=0; i<names.size(); i++){
                            if(names.get(i).equals(",") || names.get(i).equals("ou") || i == names.size()-1 ){
                                last = names.get(i-1);
                                
                                chars += (first.equals(last)) ? " SELECT id_film FROM personnes join generique on generique.id_personne = personnes.id_personne where nom LIKE \"" + first + "%\" and generique.role='R' " : " SELECT id_film FROM personnes join generique on generique.id_personne = personnes.id_personne where (nom LIKE \"" + first + "%\" AND prenom LIKE \"%"+ last + "\") OR (prenom LIKE \"" + first +"%\" AND nom LIKE \"%"+ last + "\")and generique.role='R' ";
                                if(i != names.size()-1 )
                                    first = names.get(i+1);

                                if(names.get(i).equals("ou"))
                                    chars += " UNION ";  
                                if(names.get(i).equals(","))
                                    chars += " INTERSECT ";
                                else if(i == names.size()-1)
                                    break;
                                number++;

                            }else
                                number=0;
                            last="";
                              
                        }break;

                case "AVEC":
                    dataName = entry.getValue(); 
                    if(new StringBuilder().append(dataName.charAt(0)).append(dataName.charAt(1)).toString().equals("mc"))
                        dataName = new StringBuilder(dataName).insert(1, "a").toString();

                    or = new StringBuilder().append(dataName.charAt(valueLength-1)).toString();
                    and = new StringBuilder().append(dataName.charAt(valueLength-3)).append(dataName.charAt(valueLength-2)).append(dataName.charAt(valueLength-1)).toString();
                    
                    if(!and.equals(",") && !or.equals(" ou"))
                        names = cleanArrayList(constructNameReq(dataName+ " @"));
                    else
                        names = cleanArrayList(constructNameReq(dataName));

                    first = names.get(0);
                    for(int i=0; i<names.size(); i++){
                        if(names.get(i).equals(",") || names.get(i).equals("ou") || i == names.size()-1){
                            last = names.get(i-1);

                            chars += (first == last) ? " SELECT id_film FROM personnes join generique on generique.id_personne = personnes.id_personne where nom LIKE \"" + first + "%\" AND generique.role='A'" : " SELECT id_film FROM personnes join generique on generique.id_personne = personnes.id_personne where (nom LIKE \"" + first + "%\" AND prenom LIKE \"%"+ last + "\") OR (prenom LIKE \"" + first +"%\" AND nom LIKE \"%"+ last + "\")and generique.role='A' ";

                            if(names.get(i).equals("ou"))
                                chars += " UNION ";  
                            if(i != names.size()-1)
                                first = names.get(i+1);
                            if(names.get(i).equals(","))
                                chars += " INTERSECT ";
                            else if(i == names.size()-1)
                                    break;

                            number++;
                        }else
                            number=0;
                        last="";
                        
                    }break;

                case "PAYS":
                    if(isNumeric(entry.getValue())){
                        errors.add("chiffres dans un pays");
                        break;
                    }
                    if((new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString()).equals(",") && (entry.getValue().length() - entry.getValue().replace(",", "").length()) == 1)
                        chars += " SELECT id_film FROM films, pays where films.pays= pays.code and films.pays LIKE\""+ new StringBuilder(entry.getValue()).deleteCharAt(valueLength-1).toString() + "\" OR pays.nom LIKE \""+ new StringBuilder(entry.getValue()).deleteCharAt(valueLength-1).toString() + "\" ";
                    if(!entry.getValue().contains("ou") && !entry.getValue().contains(","))
                        chars += " SELECT id_film FROM films, pays where films.pays= pays.code and films.pays LIKE \"" + entry.getValue() + "\" OR pays.nom LIKE \"" + entry.getValue() + "\" ";
                    else{
                        if(entry.getValue().contains("ou")){
                            type = new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString();
                            array = (type.equals(",")) ? get((new StringBuilder(entry.getValue()).deleteCharAt(valueLength-1).toString()).toString(), "ou") : get(entry.getValue(), "ou");
                            
                            for(int i = 0; i<array.length; i++){
                                chars += (i==0) ? " SELECT id_film FROM films, pays where films.pays= pays.code and films.pays LIKE \"" + array[i] +  "\" OR pays.nom LIKE \"" + array[i] + "\" "  : "UNION SELECT id_film FROM films, pays where films.pays = pays.code and films.pays LIKE \"" + array[i] + "\" OR pays.nom  \"" + array[i] + "\"";
                            }

                            type = new StringBuilder().append(entry.getValue().charAt(valueLength-3)).append(entry.getValue().charAt(valueLength-2)).append(entry.getValue().charAt(valueLength-1)).toString();
                            if(type.equals(" ou"))
                                    chars += " UNION "; 

                        }if(entry.getValue().contains(",")){
                            type = new StringBuilder().append(entry.getValue().charAt(valueLength-1)).toString();
                        
                            if(type.equals(",") && (entry.getValue().length() - entry.getValue().replace(",", "").length()) == 1)
                                chars += " INTERSECT "; 
                            else
                                errors.add(" et inclusif sur deux pays");
                                
                        }
                    }break;

                case "EN":
                    if(!isNumeric(entry.getValue()))
                        errors.add("lettre dans une date");

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
                                errors.add("et inclusif sur deux dates");
                                
                        }
                    }break;

                case "AVANT":
                    if(!isNumeric(entry.getValue()))
                        errors.add("mauvaise syntaxe -> lettre dans une date");
                    
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
                    if(!isNumeric(entry.getValue()))
                        errors.add("mauvaise syntaxe -> lettre dans une date");

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
        chars += ")\n " ;
        filter.append(chars + "\n\n" +sql_req);
        String final_error = "{\"errors\":";
        if(errors.size() > 0){
            //System.out.println(errors);
            for(String n : errors){
                final_error += " [" + n + "] ";
                final_error += (errors.size() > 1) ? ",": "";
            }
            return final_error + "}";
        }else{
            //System.out.println(filter.toString());
            System.out.println(request_db(filter.toString())); //System.out.println("\n\n\n" + filter);
            return "";
        }
        
    }

    public String[] get(String line, String type){
        String[] val = (type.equals("ou")) ? line.split("ou") : line.split(",");   
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
            }req=null;
        }
        return names;
    }

    public ArrayList<String> cleanArrayList(ArrayList<String> list){
        Iterator<String> it = list.iterator();
            while (it.hasNext())
            {
                if (it.next().equals(" "))
                    it.remove();     
            }return list;
    }

    public static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

    public String request_db(String user_req){
        String title="", country= "";
        int year=0, duration=0;
        ArrayList<NomPersonne> directors = new ArrayList<NomPersonne>(); //réalisateurs
        ArrayList<NomPersonne> actors = new ArrayList<NomPersonne>(); //acteurs
        ArrayList<String> o_titles = new ArrayList<>();
        NomPersonne person = new NomPersonne("", "");
        ArrayList<String> datas = new ArrayList<String>();
        System.out.println(user_req);
        try (
            
            Statement statement = _connection.createStatement();
            ResultSet rs = statement.executeQuery(user_req);
        ) {


            while ( rs.next() ) {

                String i_title = rs.getString("TITRE");
                int i_year = rs.getInt("ANNEE");
                int g_duree = rs.getInt("DUREE");
                String i_o_title = rs.getString("AUTRES_TITRES");
                String firstname = "";
                String lastname = "";
                String role = rs.getString("ROLE");
                String g_pays = rs.getString("COUNTRY");


                

                if (role.equals("A")){
                    person =new NomPersonne(lastname, firstname);
                    actors.add(person);
                }else if (role.equals("R")){
                    person =new NomPersonne(lastname, firstname);
                    directors.add(person);
                }

                title=i_title;
                year=i_year;
                duration=g_duree;
                country=g_pays;
                o_titles.add(i_o_title);

                InfoFilm movie = new InfoFilm(title, directors, actors, country, year, duration, o_titles);
                datas.add(movie.toString());
             
            }
        
            rs.close();
            statement.close();
        }catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        }
        for(String n : datas){
           System.out.println(n + "\n\n");
        }
        return datas.toString();
    }
}