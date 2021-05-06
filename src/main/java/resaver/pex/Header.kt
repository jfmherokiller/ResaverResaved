package resaver.pex;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Describes the header of a PexFile file. Useless beyond that.
 */
final public class Header {

    /**
     * Creates a Header by reading from a DataInput.
     *
     * @param input A ByteBuffer for a Skyrim PEX file.
     * @throws IOException Exceptions aren't handled.
     */
    Header(ByteBuffer input) throws IOException {
        this.magic = input.getInt();
        this.version = input.getInt();
        this.compilationTime = input.getLong();
        this.soureFilename = mf.BufferUtil.getUTF(input);
        this.userName = mf.BufferUtil.getUTF(input);
        this.machineName = mf.BufferUtil.getUTF(input);
    }

    /**
     * Write the object to a <code>ByteBuffer</code>.
     *
     * @param output The <code>ByteBuffer</code> to write.
     * @throws IOException IO errors aren't handled at all, they are simply
     *                     passed on.
     */
    public void write(ByteBuffer output) throws IOException {
        output.putInt(this.magic);
        output.putInt(this.version);
        output.putLong(this.compilationTime);
        mf.BufferUtil.putWString(output, this.soureFilename);
        mf.BufferUtil.putWString(output, this.userName);
        mf.BufferUtil.putWString(output, this.machineName);
    }

    /**
     * @return The size of the <code>Header</code>, in bytes.
     */
    public int calculateSize() {
        return 22 + this.soureFilename.length() + this.userName.length() + this.machineName.length();
    }

    /**
     * Pretty-prints the Header.
     *
     * @return A string representation of the Header.
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append(this.soureFilename)
                .append(" compiled at ")
                .append(this.compilationTime)
                .append(" by ")
                .append(this.userName)
                .append(" on ")
                .append(this.machineName)
                .append(".\n")
                .toString();
    }

    private int magic = 0;
    private int version = 0;
    public long compilationTime = 0;
    public String soureFilename = "";
    private String userName = "";
    private String machineName = "";

}
