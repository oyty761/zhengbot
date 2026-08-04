package com.guzheng.composition.controller;

import com.guzheng.common.ApiResponse;
import com.guzheng.composition.dto.CompositionDtos;
import com.guzheng.composition.service.CompositionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/composition")
@CrossOrigin
@RequiredArgsConstructor
public class CompositionController {
    private final CompositionService service;

    @GetMapping("/init") public ApiResponse<CompositionDtos.InitResponse> init(){return ApiResponse.success(service.init());}
    @PostMapping("/note") public ApiResponse<CompositionDtos.NoteResponse> add(@Valid @RequestBody CompositionDtos.NoteInput input){return ApiResponse.success(service.add(input));}
    @PutMapping("/note/{noteId}") public ApiResponse<CompositionDtos.NoteResponse> update(@PathVariable long noteId,@Valid @RequestBody CompositionDtos.NoteInput input){return ApiResponse.success(service.update(noteId,input));}
    @DeleteMapping("/note/{noteId}") public ApiResponse<CompositionDtos.MessageResponse> delete(@PathVariable long noteId,@RequestParam long compositionId){return ApiResponse.success(service.delete(noteId,compositionId));}
    @PutMapping("/{compositionId}/score") public ApiResponse<CompositionDtos.ScoreResponse> score(@PathVariable long compositionId,@Valid @RequestBody CompositionDtos.ScoreInput input){return ApiResponse.success(service.replaceScore(compositionId,input));}
    @GetMapping(value="/{compositionId}/jpt",produces="text/plain;charset=UTF-8") public ResponseEntity<String> exportJpt(@PathVariable long compositionId){return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/plain;charset=UTF-8")).body(service.exportJpt(compositionId));}
    @PutMapping(value="/{compositionId}/jpt",consumes="text/plain") public ApiResponse<CompositionDtos.ScoreResponse> importJpt(@PathVariable long compositionId,@RequestBody String text){return ApiResponse.success(service.importJpt(compositionId,text));}
    @PostMapping("/ai-completion") public ApiResponse<CompositionDtos.AiCompletionResponse> suggest(@Valid @RequestBody CompositionDtos.CompositionRequest request){return ApiResponse.success(service.suggest(request.compositionId()));}
    @PutMapping("/ai-completion/{completionId}/decision") public ApiResponse<CompositionDtos.MessageResponse> decide(@PathVariable long completionId,@Valid @RequestBody CompositionDtos.AiDecisionRequest request){return ApiResponse.success(service.decide(completionId,request));}
    @PostMapping("/complete") public ApiResponse<CompositionDtos.CompleteResponse> complete(@Valid @RequestBody CompositionDtos.CompositionRequest request){return ApiResponse.success(service.complete(request.compositionId()));}
}
