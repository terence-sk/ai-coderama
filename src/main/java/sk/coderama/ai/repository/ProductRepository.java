package sk.coderama.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sk.coderama.ai.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
