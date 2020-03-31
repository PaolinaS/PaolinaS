package softuni.exam.service.impl;

import com.google.gson.Gson;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import softuni.exam.models.dto.PictureSeedDto;
import softuni.exam.models.entities.Car;
import softuni.exam.models.entities.Picture;
import softuni.exam.repository.PictureRepository;
import softuni.exam.service.CarService;
import softuni.exam.service.PictureService;
import softuni.exam.util.ValidationUtil;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static softuni.exam.constants.GlobalConstants.*;

@Service
public class PictureServiceImpl implements PictureService {

    private final PictureRepository pictureRepository;
    private final ModelMapper modelMapper;
    private final Gson gson;
    private final ValidationUtil validationUtil;
    private final CarService carService;

    public PictureServiceImpl(PictureRepository pictureRepository, ModelMapper modelMapper, Gson gson, ValidationUtil validationUtil, CarService carService) {
        this.pictureRepository = pictureRepository;
        this.modelMapper = modelMapper;
        this.gson = gson;
        this.validationUtil = validationUtil;
        this.carService = carService;
    }


    @Override
    public boolean areImported() {
        return this.pictureRepository.count() > 0;
    }

    @Override
    public String readPicturesFromFile() throws IOException {
        return Files.readString(Path.of(PICTURE_PATH_FILE));
    }

    @Override
    public String importPictures() throws IOException {
        StringBuilder resultInfo = new StringBuilder();

        PictureSeedDto[] dtos = this.gson.fromJson(new FileReader(PICTURE_PATH_FILE), PictureSeedDto[].class);

        Arrays.stream(dtos)
                .forEach(pictureSeedDto -> {
                    if (this.validationUtil.isValid(pictureSeedDto)) {
                        if (this.pictureRepository.findByName(pictureSeedDto.getName()) == null) {

                            Picture picture = this.modelMapper.map(pictureSeedDto, Picture.class);
                            Car car = this.carService.getById(pictureSeedDto.getCar());
                            picture.setCar(car);

                            LocalDateTime dateAndTime = LocalDateTime
                                    .parse(pictureSeedDto.getDateAndTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                            picture.setDateAndTime(dateAndTime);
                            this.pictureRepository.saveAndFlush(picture);
                            resultInfo.append(String.format("Successfully import picture - %s", picture.getName()));
                            resultInfo.append(System.lineSeparator());
                        }
                    } else {
                        resultInfo.append(String.format(INCORRECT_DATA_MESSAGE, "picture"));
                        resultInfo.append(System.lineSeparator());
                    }
                });

        return resultInfo.toString();
    }

    @Override
    public List<Picture> getByCarId(Long id) {
        return this.pictureRepository.findByCarId(id);
    }
}
