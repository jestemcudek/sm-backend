package pl.edu.agh.kis.smbackend.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@NoArgsConstructor
@Table(name = "locations")
public class Location implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;
    private Double longitude;
    private Double latitude;
    private Double crowdness;

    public Location(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Location(String name, Double latitude, Double longitude){
        this.longitude=longitude;
        this.name=name;
        this.latitude=latitude;
    }
}
