package be.esi.prj.utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class JPAUtil {
    private static final EntityManagerFactory emf;
    private static final EntityManager entityManager;

    //Ce bloc de code est un bloc statique en Java. Cela signifie qu’il est exécuté une seule fois,
    //lorsque la classe est chargée par la machine virtuelle Java (JVM), avant toute instanciation de la classe.
    static {
        emf = Persistence.createEntityManagerFactory("quizAppPU");
        entityManager = emf.createEntityManager();
    }

    public static EntityManager getEntityManager() {
        return entityManager;
    }
}
