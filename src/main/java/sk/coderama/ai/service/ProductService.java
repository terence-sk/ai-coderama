package sk.coderama.ai.service;

import sk.coderama.ai.dto.request.CreateProductRequest;
import sk.coderama.ai.dto.request.UpdateProductRequest;
import sk.coderama.ai.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {

    List<ProductResponse> getAllProducts();

    ProductResponse getProductById(Long id);

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse updateProduct(Long id, UpdateProductRequest request);

    void deleteProduct(Long id);
}
