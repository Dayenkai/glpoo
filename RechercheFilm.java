import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
        String actualKey= "";
        /*
        String[] toto = {"ta", "mère", "koko"};
        String[] dodo = {"1", "2", "3", "4"};
        dodo = splice(dodo, 0, 2);
        System.out.println(Arrays.toString(dodo));
        */

        for(int i=0; i<requete.length(); i++){
            if(requete.charAt(i)== ' '){
                if(Arrays.asList(_keywords).contains(word)){
                    data.put(word.toLowerCase(), null);
                    actualKey = word;
                }else{
                    data.replace(actualKey, null, word.toLowerCase());
                    actualKey="";
                }
                word = "";
            }

            word += requete.charAt(i);


        }

        for (String name: data.keySet()){
            String key = name;
            String value = data.get(name);
            System.out.println(key + " " + value + "\n");
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