package com.eventixx.eventcatalog.services.category;

import com.eventixx.eventcatalog.config.MapStructConfig;
import com.eventixx.eventcatalog.dto.category.CategoryResponse;
import com.eventixx.eventcatalog.dto.category.CategorySummaryResponse;
import com.eventixx.eventcatalog.dto.category.CreateCategoryRequest;
import com.eventixx.eventcatalog.dto.category.UpdateCategoryRequest;
import com.eventixx.eventcatalog.entities.Category;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface CategoryMapper {

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  Category toEntity(CreateCategoryRequest request);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  void updateEntity(UpdateCategoryRequest request, @MappingTarget Category category);

  CategoryResponse toResponse(Category category);

  CategorySummaryResponse toSummary(Category category);

  List<CategorySummaryResponse> toSummaryList(List<Category> categories);
}
