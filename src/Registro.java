public class Registro {
    private String nombreDominio;
    private String tipoRegistro;
    private String valor;

    public Registro(String nombreDominio, String tipoRegistro, String valor) {
        this.nombreDominio = nombreDominio;
        this.tipoRegistro = tipoRegistro;
        this.valor = valor;
    }

    public String getNombreDominio() {
        return nombreDominio;
    }

    public void setNombreDominio(String nombreDominio) {
        this.nombreDominio = nombreDominio;
    }

    public String getTipoRegistro() {
        return tipoRegistro;
    }

    public void setTipoRegistro(String tipoRegistro) {
        this.tipoRegistro = tipoRegistro;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public String toString() {
        return nombreDominio + " " + tipoRegistro + " " + valor;
    }
}
