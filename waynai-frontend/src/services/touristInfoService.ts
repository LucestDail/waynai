import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export interface AreaCodeDto {
  areaCode: string;
  areaName: string;
  sigunguCode: string;
  sigunguName: string;
}

export interface TouristSpotDto {
  baseYm: string;
  mapX: string;
  mapY: string;
  areaCd: string;
  areaNm: string;
  signguCd: string;
  signguNm: string;
  hubTatsCd: string;
  hubTatsNm: string;
  hubCtgryLclsNm: string;
  hubCtgryMclsNm: string;
  hubCtgrySclsNm: string;
  hubRank: string;
}

export interface TouristSpotResponseDto {
  success: boolean;
  resultCode: string;
  resultMsg: string;
  pageNo: number;
  numOfRows: number;
  totalCount: number;
  itemCount: number;
  items: TouristSpotDto[] | null;
}

class TouristInfoService {
  async searchAreaByName(areaName: string): Promise<AreaCodeDto[]> {
    const { data } = await axios.get<AreaCodeDto[]>(`${API_BASE_URL}/api/tourist/area-codes/search`, {
      params: { areaName },
    });
    return data ?? [];
  }

  async getSpots(
    areaCode: string,
    sigunguCode: string,
    pageNo = 1,
    numOfRows = 20,
  ): Promise<TouristSpotResponseDto> {
    const { data } = await axios.get<TouristSpotResponseDto>(`${API_BASE_URL}/api/tourist/spots`, {
      params: { areaCode, sigunguCode, pageNo, numOfRows },
    });
    return data;
  }
}

export default new TouristInfoService();
