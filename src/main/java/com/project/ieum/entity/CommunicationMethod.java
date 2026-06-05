package com.project.ieum.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "communication_methods",
    uniqueConstraints = @UniqueConstraint(name = "uq_cm_name", columnNames = "name_ko"))
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class CommunicationMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name_ko", nullable = false, length = 40)
    private String nameKo;

    @Column(name = "description", length = 100)
    private String description;

    @Column(name = "sort_order")
    private Short sortOrder;
}
