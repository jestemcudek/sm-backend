package pl.edu.agh.kis.smbackend.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@NoArgsConstructor
@Table(name = "users")
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String email;
    private String password;
    private String name;
    private String surname;
    @OneToOne
    private Location currentLocation;

    public User(String email, String name, String surname, String password) {
        this.email = email;
        this.name = name;
        this.surname = surname;
        this.password = password;
    }


}
