package pl.edu.agh.kis.smbackend.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.edu.agh.kis.smbackend.model.Location;

import java.util.Optional;

@Repository
public interface LocationDAO extends JpaRepository<Location, Integer> {
    Optional<Location> findByLatitudeAndLongitude(double latitude, double longitude);
}
