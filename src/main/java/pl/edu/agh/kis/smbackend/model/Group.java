package pl.edu.agh.kis.smbackend.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "groups")
public class Group implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String groupName;
    private boolean isSelected;
    private String ownerFirstName;
    private String ownerLastName;
    @OneToMany
    private List<User> groupMembers;

    public Group(String groupName, List<User> members) {
        this.groupMembers = members;
        this.groupName = groupName;
    }
}
