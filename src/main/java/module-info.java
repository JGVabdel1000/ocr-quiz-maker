module be.esi.prj {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires org.slf4j;
    requires tess4j;
    requires io.github.cdimascio.dotenv.java;
    requires java.net.http;
    requires com.google.gson;

    opens be.esi.prj to javafx.fxml, org.hibernate.orm.core;
    opens be.esi.prj.viewmodel to javafx.fxml;
    opens be.esi.prj.model.orm to org.hibernate.orm.core;

    exports be.esi.prj;
    exports be.esi.prj.viewmodel;
    exports be.esi.prj.model.orm;
}
