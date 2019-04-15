/**
 *    Gestion des noms (noms de famille + pr&eacute;nom) des personnes.
 *    <p>
 *    La classe NomPersonne permet de g&eacute;rer les noms et tient
 *    en particulier compte des pr&eacute;fixes des noms ('de', 'von', 'van')
 *    dans le tri.
 */ 
public class NomPersonne implements Comparable<NomPersonne>{
    private String _nom;
    private String _prenom;
    private int    _debutComp;

    /**
     *    Cr&eacute; d'un nouveau NomPersonne. Attention, le pr&eacute;nom
     *    est pass&eacute; en deuxi&egrave;me.
     *
     *    @param nom Nom de famille ou nom d'artiste
     *    @param prenom Pr&eacute;nom (peut &ecirc;tre "null")
     */
    public NomPersonne(String nom, String prenom) {
        _nom = new String(nom);
        _prenom = new String(prenom);
        _debutComp = 0;
        // On regarde quel est le premier caractère en majuscules
        // pour trier 'von Stroheim' avec les S, 'de la Huerta'
        // avec les H et 'de Funès' avec les F.
        // 'De Niro' sera en revanche à D.
        while ((_debutComp < _nom.length())
               && (_nom.charAt(_debutComp)
                   == Character.toLowerCase(_nom.charAt(_debutComp)))) {
           _debutComp++;
        }
    }

    /**   Comparateur qui tient compte des pr&eacute;fixes de noms.
     *    
     *    @param autre NomPersonne qui est compar&eacute; &agrave; l'objet courant
     *    @return un entier inf&eacute;rieur, &eacute;gal ou sup&eacute;rieur &agrave; z&eacute;ro suivant le cas
     */
    @Override
    public int compareTo(NomPersonne autre) {
        if (autre == null) {
          return 1;
        }
        int cmp = this._nom.substring(this._debutComp)
                      .compareTo(autre._nom.substring(autre._debutComp));
        if (cmp == 0) {
          return this._prenom.compareTo(autre._prenom);
        } else {
          return cmp;
        }
    }
    
    /**
     *   Retourne un nom affichable.
     *   <p>
     *   S'il y a une mention telle que (Jr.) qui dans la base est dans
     *   la colonne du pr&eacute;nom, elle est report&eacute;e &agrave; 
     *   la fin.
     *
     *   @return La combinaison du pr&eacute;nom et du nom, dans cet ordre.
     */
    @Override
    public String toString() {
        int pos = -1;

        if (this._prenom != null) {
          // Les mentions entre parenthèses seront renvoyées
          // à la fin.
          pos = this._prenom.indexOf('(');
        }
        if (pos == -1) {
          if (this._prenom == null) {
            return this._nom;
          } else {
            return this._prenom + " " + this._nom;
          }
        } else {
          return this._prenom.substring(0, pos-1).trim() 
                 + " " + this._nom
                 + " " + this._prenom.substring(pos).trim();
        }
    }
}