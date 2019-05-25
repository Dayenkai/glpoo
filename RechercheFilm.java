import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/***
 * @author Joseph-Emmanuel Banzio
 * Classe RechercheFilm permettant d'effectuer une recherche sur un film en particulier en fonction de mots clés
 */

public class RechercheFilm {
    private Connection _connection;
    private String[] _keywords = {"TITRE", "DE", "AVEC", "PAYS", "EN", "AVANT", "APRES"};

    /**
     * Constructeur
     * Ouvre la base
     * @param nomFichierSQLite
     */

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
    /**
     * Permet de fermer la base de données
     */
    public void fermeBase(){
        try {
            _connection.close();
            System.out.println("The database has been closed.\n");
        }catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Retourne la réponse de la requête utilisateur sous le format JSON
     * La fonction parcourt la chaine de caractères et effectue progressivement son traitement
     * => Si c'est un keyword, il sera stocké comme clé d'une LinkedHashMap pour conserver l'ordre d'insertion
     * => Sinon, il stocke la donnée comme valeur correspondante à la clé
     * @param requete chaine de caractères contenant la requête utilisateur (ex: titre avengers)
     * @return réponse à la requête soit un resultat soit une erreur
     */

    public String retrouve(String requete){
        LinkedHashMap<String, String> data = new LinkedHashMap<String, String>();
        String word = "";
        String actualKey= "", tKey="";
        requete +=" ";
       
        for(int i=0; i<requete.length(); i++){
            if(requete.trim().length() == 0)
                return "{\"erreurs\": [ne contient que des espaces] }";
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

    /***
     * La fonction prend la hashmap qu'elle parcourt du premier au dernier élément inséré 
     * pour éviter que les liens (ou et ,) ne soient pas pris en compte
     * Elle construit progressivement le filtre selon les clés de la hashmap 
     * Elle fait ensuite appel à la fonction requestDB()
     * @param user_req requête utilisateur sous la format d'une linkedhashmap
     * @return le string retourné par requestDB()
     */

    public String analyzeRequest(LinkedHashMap<String, String> user_req){
        StringBuilder sql_req = new StringBuilder("select F.titre,F.annee, F.duree, group_concat(A.titre, '|') as autres_titres,P.prenom, P.nom, G.role, Py.nom as country from filtre\n" +
        "                                                                                                              join films f on F.id_film = filtre.id_film\n" +
        "                                                                                                              join pays py on Py.code = F.pays\n" +
        "                                                                                                              left join autres_titres a on A.id_film = F.id_film\n" +
        "                                                                                                              join generique g on G.id_film = F.id_film\n" +
        "                                                                                                              join personnes p on P.id_personne = G.id_personne\n" +
        "group by F.titre,F.annee, F.duree, P.prenom, P.nom, G.role, F.pays ");
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
                        chars += " SELECT id_film FROM recherche_titre WHERE titre MATCH \"" + new StringBuilder(entry.getValue()).deleteCharAt(valueLength-1).toString() + "\" "; 
                    if(!entry.getValue().contains("ou") && !entry.getValue().contains(","))
                        chars += " SELECT id_film FROM recherche_titre WHERE titre MATCH \"" + entry.getValue() + "\" ";
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
                        else 
                            chars += " INTERSECT ";
            }

        }
        chars += ")\n " ;
        filter.append(chars + "\n\n" +sql_req);
        String final_error = "{\"errors\":";
        if(errors.size() > 0){
            for(String n : errors){
                final_error += " [" + n + "] ";
                final_error += (errors.size() > 1) ? ",": "";
            }
            return final_error + "}";
        }else
            return requestDB(filter.toString());
        
        
    }

    /***
     * Permet de split selon un séparateur
     * @param line chaine de caractères qui correspond à la valeur de la hashmap
     * @param type séparateur (ou ,)
     * @return tableau de string
     */

    public String[] get(String line, String type){
        String[] val = (type.equals("ou")) ? line.split("ou") : line.split(",");   
        return val;
    }
   
    /***
    * Permet de spliter l'information quand l'utilisateur saisit "de" ou "avec"
    * Permet donc de gérer les noms dans le cas de n termes
    * @param req chaine de caractères qui correspond à la valeur de la hashmap
    * @return une arraylist de string qui contient tous les noms qui ont été splité
    */

    public ArrayList<String> constructNameReq(String req){ 
        StringTokenizer st;
        int i=0;
        String word = "", line = "";
        ArrayList<String> names = new ArrayList<String>();
        
        while ((line = req) != null) {
            st = new StringTokenizer(line, ", ", true);
            while (st.hasMoreTokens()) {
                word = st.nextToken().toLowerCase();
                names.add(i, word);
                i++;
            }req=null;
        }
        return names;
    }
    
    /***
     * Permet de vérifier si une arraylist ne contient pas d'espaces
     * Dans le cas où elle contient des espaces, elle les supprime
     * @param list liste de noms qui ont été splité auparavant
     * @return une arraylist de string sans espaces comme paramètres
     */

    public ArrayList<String> cleanArrayList(ArrayList<String> list){
        Iterator<String> it = list.iterator();
            while (it.hasNext())
            {
                if (it.next().equals(" "))
                    it.remove();     
            }return list;
    }

    /***
     * Vérifie si la chaine de caractères envoyée est une valeur numérique ou non avec les expressions régulières
     * 
     * @param str chaine de caractères à vérifier
     * @return true ou false selon l'issue
     */
    public static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");  
    }

    /***
     * Interagit avec la base de données en effectuant la requête qui a été construite
     * On caste le résultat de la requête pour ensuite créer des instances de InfoFilm dans un try{}catch
     * On construit ici progressivement le résultat sous le format JSON avec la méthode toString() de InfoFilm
     * On fait également en sorte que la valeur de retour ne contient que les films totalement complétés
     * @param user_req requête SQL complète de l'utilisateur avec le filtre
     * @return réponse sous format JSON
     * @throws Exception liée à SQL
     */

    public String requestDB(String user_req){
        String title="", country= "",  resReq = "{\"resultat:\" ", o_title="";
        int year=0, duration=0, i=0, movieLimit=0;
        ArrayList<NomPersonne> directors = new ArrayList<NomPersonne>() ; 
        ArrayList<NomPersonne> actors = new ArrayList<NomPersonne>();
        ArrayList<String> o_titles = new ArrayList<String>();
        try (
            Statement statement = _connection.createStatement();
            ResultSet rs = statement.executeQuery(user_req);
        ) {

            while (rs.next()) {
                if(!title.equals(null) && !(rs.getString("TITRE").equals(title))){
                    InfoFilm movie = new InfoFilm(title, directors, actors, country, year, duration, o_titles);
                    resReq += "\n" + movie.toString() ;
                   

                    directors = new ArrayList<NomPersonne>();
                    actors = new ArrayList<NomPersonne>();
                    o_titles = new ArrayList<String>();
                    i=0;
                    movieLimit++;
                    
                    if(movieLimit==100)
                        break;
                    else
                        resReq += ",";     
                }
                String lastname = "", firstname = "", role = "";
                title = rs.getString("TITRE");
                year = rs.getInt("ANNEE");
                duration = rs.getInt("DUREE");
                o_title = rs.getString("AUTRES_TITRES");
                role = rs.getString("ROLE");
                country = rs.getString("COUNTRY");

                
                o_titles.add(o_title);

                if(!rs.getString("NOM").equals(null))
                   lastname = rs.getString("NOM");
                if(!rs.getString("PRENOM").equals(null))
                   firstname = rs.getString("PRENOM");
            
                
                if(role.equals("A"))
                    actors.add(new NomPersonne(lastname, firstname));
                if(role.equals("R"))
                    directors.add(new NomPersonne(lastname, firstname));

                if(!rs.getString("AUTRES_TITRES").equals(null)){
                    ArrayList<String> o_movies = new ArrayList<String>(Arrays.asList(rs.getString("AUTRES_TITRES").split("%")));

                    for(int cp=0; cp < o_movies.size(); cp++){
                        if(i==0)
                            o_titles.add(o_movies.get(cp));
                    }
                    i++;
                }

            }if(!title.equals(null) && movieLimit!=100){
                InfoFilm res = new InfoFilm(title, directors, actors, country, year, duration, o_titles);
                movieLimit++;
                resReq += "\n" +res.toString() ;
            }

            resReq +=  (movieLimit == 100) ? "], \"info:\" 100 FILMS" : "]}";

            } catch ( Exception e ) {
                System.err.println( e.getMessage() );
            }
        return resReq + "\n";
    }
}