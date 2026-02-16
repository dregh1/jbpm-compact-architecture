package mg.orange.workflow.model.role;

import java.util.List;

public class RoleMapper {
    private RoleMapper(){}

    public static RoleDTO toDTO(Role role){
        if(role == null) return null;

        RoleDTO dto = new RoleDTO();
        dto.setIdRole(role.getIdRole());
        dto.setNom(role.getNom());

        return dto;
    }

    public static List<RoleDTO> toDTOList(List<Role> roles) {
        return roles.stream()
                .map(RoleMapper::toDTO)
                .toList();
    }
}
