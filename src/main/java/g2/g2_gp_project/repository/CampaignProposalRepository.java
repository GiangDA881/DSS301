package g2.g2_gp_project.repository;

import g2.g2_gp_project.entity.CampaignProposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignProposalRepository extends JpaRepository<CampaignProposal, Integer> {
}
