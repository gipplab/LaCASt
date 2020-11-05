package gov.nist.drmf.interpreter.pom.moi;

/**
 * @author Andre Greiner-Petter
 */
public class Connection {
    final String in, out;

    public Connection(String in, String out) {
        this.in = in;
        this.out = out;
    }

    @Override
    public int hashCode() {
        int result = this.in != null ? this.in.hashCode() : 0;
        result = 31 * result + (this.out != null ? this.out.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if ( obj == null ) return false;
        if ( obj instanceof Connection ) {
            Connection ref = (Connection) obj;
            return in.equals(ref.in) && out.equals(ref.out);
        } else return false;
    }
}
