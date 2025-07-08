export interface SearchRequest {
  query: string;
  searchType?: 'keyword' | 'sentence';
  destination?: string;
  theme?: string;
  days?: number;
  budget?: string;
  transportation?: string;
  travelStyle?: string;
}

export interface SearchResponse {
  status: 'analyzing' | 'enhancing' | 'searching' | 'generating' | 'completed' | 'error';
  message: string;
  step: 'intent_analysis' | 'query_enhancement' | 'knowledge_search' | 'course_generation' | 'error';
  data: any;
  timestamp: string;
  progress: string[];
}

export interface SearchState {
  isSearching: boolean;
  currentStatus: string;
  currentStep: string;
  progress: string[];
  result: any;
  error: string | null;
} 