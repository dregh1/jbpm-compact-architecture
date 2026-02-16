package mg.orange.workflow.model.process;

import org.jboss.resteasy.annotations.providers.multipart.PartType;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import java.io.InputStream;

public class BpmnUploadForm {

    @FormParam("file")
    @PartType(MediaType.APPLICATION_OCTET_STREAM)
    private InputStream file;

    @FormParam("filename")
    @PartType(MediaType.TEXT_PLAIN)
    private String filename;

    @FormParam("overwrite")
    @PartType(MediaType.TEXT_PLAIN)
    private Boolean overwrite;

    // Getters et setters
    public InputStream getFile() {
        return file;
    }

    public void setFile(InputStream file) {
        this.file = file;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Boolean getOverwrite() {
        return overwrite;
    }

    public void setOverwrite(Boolean overwrite) {
        this.overwrite = overwrite;
    }
}