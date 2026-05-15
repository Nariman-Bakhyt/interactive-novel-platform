export interface PagedModel<T> {
  // Соответствует @JsonProperty над методом getContent()
  content: T[];

  // Соответствует @JsonProperty("page") над методом getMetadata()
  page: PageMetadata;
}

// Соответствует Java record PageMetadata
export interface PageMetadata {
  number: number;     // Номер текущей страницы (начиная с 0)
  size: number;      // Количество элементов на странице
  totalElements: number; // Всего записей в базе
  totalPages: number;    // Всего страниц
}
