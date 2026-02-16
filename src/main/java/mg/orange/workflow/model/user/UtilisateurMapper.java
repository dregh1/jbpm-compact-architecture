package mg.orange.workflow.model.user;

import mg.orange.workflow.model.role.Role;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class UtilisateurMapper {

    private UtilisateurMapper(){}

    public static UtilisateurDTO toDTO(Utilisateur utilisateur) {
        if(utilisateur == null) return null;

        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setIdUtilisateur(utilisateur.getIdUtilisateur());
        dto.setNom(utilisateur.getNom());
        dto.setTrigram(utilisateur.getTrigram());
        dto.setEmail(utilisateur.getEmail());
        dto.setNumero(utilisateur.getNumero());
        if(utilisateur.getRole() != null) {
            dto.setRole(utilisateur.getRole());
        }
        return dto;
    }

    public static List<UtilisateurDTO> toDTOList(List<Utilisateur> utilisateurs) {
        return utilisateurs.stream()
                .map(UtilisateurMapper::toDTO)
                .collect(Collectors.toList());
    }

    public static List<UtilisateurDTO> toDTOList(Set<Utilisateur> utilisateurs) {
        return utilisateurs.stream()
                .map(UtilisateurMapper::toDTO)
                .collect(Collectors.toList());
    }

    public static Utilisateur userRequestToEntity(UtilisateurRegisterDTO utilisateurRegisterDTO, Role role) {
        if(utilisateurRegisterDTO == null) return null;

        Utilisateur entity = new Utilisateur();
        entity.setNom(utilisateurRegisterDTO.getNom());
        entity.setMdp(utilisateurRegisterDTO.getTrigram());
        entity.setTrigram(utilisateurRegisterDTO.getTrigram());
        entity.setEmail(utilisateurRegisterDTO.getEmail());
        entity.setNumero(utilisateurRegisterDTO.getNumero());
        entity.setRole(role);
        return entity;
    }

    public static Utilisateur userGroupeDTOToEntity(UtilisateurGroupeDTO utilisateurGroupeDTO) {
        if(utilisateurGroupeDTO == null) return null;

        Utilisateur entity = new Utilisateur();
        entity.setIdUtilisateur(utilisateurGroupeDTO.getIdUtilisateur());
        entity.setNom(utilisateurGroupeDTO.getNom());
        entity.setTrigram(utilisateurGroupeDTO.getTrigram());
        entity.setEmail(utilisateurGroupeDTO.getEmail());
        entity.setNumero(utilisateurGroupeDTO.getNumero());
        return entity;
    }

}
