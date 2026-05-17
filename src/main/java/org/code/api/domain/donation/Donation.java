package org.code.api.domain.donation;

import jakarta.persistence.*;


@Entity
@Table(name = "donations")
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String donorName;
    private String donorDocument;
    private String donorType;
    private String donorAddress;
    private Long materialTypeId;
    private Long materialSubtypeId;
    private Long materialSubSubtypeId;
    private Double weight;

    public Donation() {
    }

    public Long getId() {
        return id;
    }

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public String getDonorDocument() {
        return donorDocument;
    }

    public void setDonorDocument(String donorDocument) {
        this.donorDocument = donorDocument;
    }

    public String getDonorType() {
        return donorType;
    }

    public void setDonorType(String donorType) {
        this.donorType = donorType;
    }

    public String getDonorAddress() {
        return donorAddress;
    }

    public void setDonorAddress(String donorAddress) {
        this.donorAddress = donorAddress;
    }

    public Long getMaterialTypeId() {
        return materialTypeId;
    }

    public void setMaterialTypeId(Long materialTypeId) {
        this.materialTypeId = materialTypeId;
    }

    public Long getMaterialSubtypeId() {
        return materialSubtypeId;
    }

    public void setMaterialSubtypeId(Long materialSubtypeId) {
        this.materialSubtypeId = materialSubtypeId;
    }

    public Long getMaterialSubSubtypeId() {
        return materialSubSubtypeId;
    }

    public void setMaterialSubSubtypeId(Long materialSubSubtypeId) {
        this.materialSubSubtypeId = materialSubSubtypeId;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }
}
