package com.jobagent.service;

import com.jobagent.dto.*;
import com.jobagent.model.*;
import com.jobagent.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileDataService {

    private final EducationRepository educationRepository;
    private final CertificationRepository certificationRepository;
    private final UserProfileLinkRepository linkRepository;

    @Transactional(readOnly = true)
    public List<EducationResponse> getEducation(UUID userId) {
        return educationRepository.findByUserIdOrderByStartDateDesc(userId).stream()
                .map(e -> new EducationResponse(e.getId(), e.getInstitution(), e.getDegree(),
                        e.getFieldOfStudy(), e.getStartDate(), e.getEndDate(), e.getDescription()))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<EducationResponse> saveEducation(UUID userId, SaveEducationRequest request) {
        educationRepository.findByUserIdOrderByStartDateDesc(userId)
                .forEach(educationRepository::delete);

        User user = new User();
        user.setId(userId);

        List<Education> saved = request.education().stream()
                .map(item -> educationRepository.save(Education.builder()
                        .user(user)
                        .institution(item.institution())
                        .degree(item.degree())
                        .fieldOfStudy(item.fieldOfStudy())
                        .startDate(item.startDate())
                        .endDate(item.endDate())
                        .description(item.description())
                        .build()))
                .collect(Collectors.toList());

        return saved.stream()
                .map(e -> new EducationResponse(e.getId(), e.getInstitution(), e.getDegree(),
                        e.getFieldOfStudy(), e.getStartDate(), e.getEndDate(), e.getDescription()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CertificationResponse> getCertifications(UUID userId) {
        return certificationRepository.findByUserIdOrderByIssueDateDesc(userId).stream()
                .map(c -> new CertificationResponse(c.getId(), c.getName(), c.getIssuingOrg(),
                        c.getIssueDate(), c.getExpiryDate(), c.getCredentialUrl()))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<CertificationResponse> saveCertifications(UUID userId, SaveCertificationRequest request) {
        certificationRepository.findByUserIdOrderByIssueDateDesc(userId)
                .forEach(certificationRepository::delete);

        User user = new User();
        user.setId(userId);

        List<Certification> saved = request.certifications().stream()
                .map(item -> certificationRepository.save(Certification.builder()
                        .user(user)
                        .name(item.name())
                        .issuingOrg(item.issuingOrg())
                        .issueDate(item.issueDate())
                        .expiryDate(item.expiryDate())
                        .credentialUrl(item.credentialUrl())
                        .build()))
                .collect(Collectors.toList());

        return saved.stream()
                .map(c -> new CertificationResponse(c.getId(), c.getName(), c.getIssuingOrg(),
                        c.getIssueDate(), c.getExpiryDate(), c.getCredentialUrl()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProfileLinkResponse> getLinks(UUID userId) {
        return linkRepository.findByUserId(userId).stream()
                .map(l -> new ProfileLinkResponse(l.getId(), l.getLinkType(), l.getUrl(), l.getLabel()))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<ProfileLinkResponse> saveLinks(UUID userId, SaveLinksRequest request) {
        linkRepository.findByUserId(userId).forEach(linkRepository::delete);

        User user = new User();
        user.setId(userId);

        List<UserProfileLink> saved = request.links().stream()
                .map(item -> linkRepository.save(UserProfileLink.builder()
                        .user(user)
                        .linkType(item.linkType())
                        .url(item.url())
                        .label(item.label())
                        .build()))
                .collect(Collectors.toList());

        return saved.stream()
                .map(l -> new ProfileLinkResponse(l.getId(), l.getLinkType(), l.getUrl(), l.getLabel()))
                .collect(Collectors.toList());
    }
}
