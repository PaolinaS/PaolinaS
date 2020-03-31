package softuni.exam.service.impl;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import softuni.exam.models.dto.OfferSeedRootDto;
import softuni.exam.models.entities.Car;
import softuni.exam.models.entities.Offer;
import softuni.exam.models.entities.Picture;
import softuni.exam.models.entities.Seller;
import softuni.exam.repository.OfferRepository;
import softuni.exam.service.CarService;
import softuni.exam.service.OfferService;
import softuni.exam.service.PictureService;
import softuni.exam.service.SellerService;
import softuni.exam.util.ValidationUtil;
import softuni.exam.util.XmlParser;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;

import static softuni.exam.constants.GlobalConstants.INCORRECT_DATA_MESSAGE;
import static softuni.exam.constants.GlobalConstants.OFFER_PATH_FILE;

@Service
public class OfferServiceImpl implements OfferService {

    private final OfferRepository offerRepository;
    private final ModelMapper modelMapper;
    private final ValidationUtil validationUtil;
    private final XmlParser xmlParser;
    private final CarService carService;
    private final SellerService sellerService;
    private final PictureService pictureService;

    public OfferServiceImpl(OfferRepository offerRepository, ModelMapper modelMapper, ValidationUtil validationUtil, XmlParser xmlParser, CarService carService, SellerService sellerService, PictureService pictureService) {
        this.offerRepository = offerRepository;
        this.modelMapper = modelMapper;
        this.validationUtil = validationUtil;
        this.xmlParser = xmlParser;
        this.carService = carService;
        this.sellerService = sellerService;
        this.pictureService = pictureService;
    }


    @Override
    public boolean areImported() {
        return this.offerRepository.count() > 0;
    }

    @Override
    public String readOffersFileContent() throws IOException {
        return Files.readString(Path.of(OFFER_PATH_FILE));
    }

    @Override
    public String importOffers() throws IOException, JAXBException {
        StringBuilder resultInfo = new StringBuilder();

        OfferSeedRootDto offerSeedRootDto = this.xmlParser
                .parseXml(OfferSeedRootDto.class, OFFER_PATH_FILE);

        offerSeedRootDto.getOffers()
                .forEach(offerSeedDto -> {
                    if (this.validationUtil.isValid(offerSeedDto)){
                        LocalDateTime addedOn =
                                LocalDateTime.parse(offerSeedDto.getAddedOn(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        if (this.offerRepository.findByDescriptionAndAddedOn(offerSeedDto.getDescription(), addedOn) == null) {

                            Offer offer = this.modelMapper.map(offerSeedDto, Offer.class);
                            Car car = this.carService.getById(offerSeedDto.getCar().getId());
                            Seller seller = this.sellerService.getById(offerSeedDto.getSeller().getId());
                            List<Picture> pictures = this.pictureService.getByCarId(offerSeedDto.getCar().getId());
                            offer.setPictures(new HashSet<>(pictures));
                            offer.setCar(car);
                            offer.setSeller(seller);
                            offer.setAddedOn(addedOn);
                            this.offerRepository.saveAndFlush(offer);
                            resultInfo.append(String.format("Successfully import offer %s - %s", offer.getAddedOn().toString(), offer.getHasGoldStatus()))
                                    .append(System.lineSeparator());
                        }
                    } else {
                        resultInfo.append(String.format(INCORRECT_DATA_MESSAGE, "offer"))
                                .append(System.lineSeparator());
                    }
                });


        return resultInfo.toString();
    }
}
