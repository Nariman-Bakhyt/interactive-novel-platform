export interface PagedModel<T> {
  
  content: T[];

  
  page: PageMetadata;
}


export interface PageMetadata {
  number: number;     
  size: number;      
  totalElements: number; 
  totalPages: number;    
}

export interface SliceModel<T> {
  content: T[];
  last: boolean;
  first: boolean;
  number: number;
  size: number;
  numberOfElements: number;
  empty: boolean;
}

