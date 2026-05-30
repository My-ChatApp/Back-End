package iuh.fit.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.UUID;

@Entity
@Table(name = "users", schema = "app")
@Getter
public class UserPresence {

    @Id
    private UUID id;

    @Column(nullable = false)
    private boolean online;
}
