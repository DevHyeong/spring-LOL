package kr.gg.lol.domain.summoner.service;

import kr.gg.lol.common.util.Rest;
import kr.gg.lol.domain.summoner.dto.LeagueDto;
import kr.gg.lol.domain.summoner.dto.SummonerDto;
import kr.gg.lol.domain.summoner.entity.League;
import kr.gg.lol.domain.summoner.entity.Summoner;
import kr.gg.lol.domain.summoner.repository.LeagueRepository;
import kr.gg.lol.domain.summoner.repository.SummonerJdbcRepository;
import kr.gg.lol.domain.summoner.repository.SummonerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static kr.gg.lol.domain.summoner.dto.LeagueDto.toDto;

@Service
@RequiredArgsConstructor
public class SummonerService {
    private final Rest rest;
    private final SummonerRepository summonerRepository;
    private final SummonerJdbcRepository summonerJdbcRepository;
    private final LeagueRepository leagueRepository;

    public ResponseEntity getSummonerByName(String name){

        Optional<Summoner> summoner = summonerRepository.findByName(name);

        if(summoner.isEmpty()){

            URI uri = UriComponentsBuilder
                    .fromUriString("https://kr.api.riotgames.com")
                    .path("/lol/summoner/v4/summoners/by-name/{name}")
                    .encode()
                    .build()
                    .expand(name)
                    .toUri();

            ResponseEntity<SummonerDto> response = rest.get(uri, SummonerDto.class);
            saveSummoner(response);
            return response;
        }
        return ResponseEntity.ok(SummonerDto.toDto(summoner.get()));
    }

    public ResponseEntity getLeagueById(String id){
        Optional<List<League>> leagues = leagueRepository.findBySummonerId(id);
        if(leagues.isEmpty()){

            URI uri = UriComponentsBuilder
                    .fromUriString("https://kr.api.riotgames.com")
                    .path("/lol/league/v4/entries/by-summoner/{id}")
                    .encode()
                    .build()
                    .expand(id)
                    .toUri();

            ResponseEntity<List<LeagueDto>> response = rest.get(uri, new ParameterizedTypeReference<List<LeagueDto>>() {});
            saveLeagues(response);
            return response;
        }
        return ResponseEntity.ok(toDto(leagues.get()));
    }

    private void saveSummoner(ResponseEntity<SummonerDto> response){
        Assert.notNull(response, "");

        SummonerDto dto = response.getBody();
        Summoner summoner = Summoner.builder()
                .accountId(dto.getAccountId())
                .puuid(dto.getPuuid())
                .name(dto.getName())
                .profileIconId(dto.getProfileIconId())
                .summonerLevel(dto.getSummonerLevel())
                .id(dto.getId())
                .build();

        summonerRepository.save(summoner);

    }

    private void saveLeagues(ResponseEntity<List<LeagueDto>> response){
        Assert.notNull(response, "");

        List<LeagueDto> leagueDtos = response.getBody();
        List<League> leagues = new ArrayList<>();
        for(LeagueDto dto : leagueDtos){

            League league = League.builder()
                    .queueType(dto.getQueueType())
                    .summonerId(dto.getSummonerId())
                    .summonerName(dto.getSummonerName())
                    .leagueId(dto.getLeagueId())
                    .rank(dto.getRank())
                    .tier(dto.getTier())
                    .wins(dto.getWins())
                    .losses(dto.getLosses())
                    .build();

            leagues.add(league);
        }
        summonerJdbcRepository.bulkInsert(leagues);

    }











}
